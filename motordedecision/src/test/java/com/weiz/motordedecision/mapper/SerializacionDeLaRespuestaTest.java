package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.crearSolicitudConLaCadenaCompleta;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.crearSolicitudRechazadaPorDocumentoBloqueado;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba el JSON que sale de verdad por el cable: se serializa con el
 * {@code ObjectMapper} de la aplicacion y con el mapeador armado desde
 * application.yml, no con uno de mentira.
 *
 * El caso aprobado se compara contra el JSON literal del enunciado (lineas
 * 96-124), arbol contra arbol. La unica diferencia deliberada es el detalle de
 * la validacion de Score, que la cadena escribe con su umbral real
 * ({@code "Score 750 >= 600"}, commit 4767e0a): ese texto lo produce la feature
 * 10 y la respuesta se limita a repetirlo.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Serializacion JSON de la respuesta")
class SerializacionDeLaRespuestaTest {

    private static final String JSON_ESPERADO_DEL_ENUNCIADO = """
            {
              "idSolicitud": "SOL-20260824-001",
              "fechaCreacion": "2026-08-24T10:30:00",
              "estado": "APROBADO",
              "solicitante": {
                "nombre": "Juan Pérez",
                "documento": "CC 1234567890"
              },
              "detalle": {
                "montoSolicitado": 15000000,
                "plazoMeses": 36,
                "tasaEstimada": 1.2
              },
              "evaluacion": {
                "scoreBureau": 750,
                "validaciones": [
                  { "nombre": "Identidad", "resultado": "APROBADO", "detalle": "Documento no bloqueado" },
                  { "nombre": "Score", "resultado": "APROBADO", "detalle": "Score 750 >= 600" },
                  { "nombre": "Capacidad de pago", "resultado": "APROBADO", "detalle": "Monto dentro del rango permitido" },
                  { "nombre": "Reporte negativo", "resultado": "APROBADO", "detalle": "Sin reportes negativos" }
                ]
              },
              "siguientePaso": "Se enviará contrato al correo registrado en 24 horas"
            }
            """;

    private static final String CAMPO_TASA = "tasaEstimada";
    private static final String CAMPO_SIGUIENTE_PASO = "siguientePaso";

    @Autowired
    private MapeadorDeSolicitud mapeador;

    @Autowired
    private ObjectMapper serializador;

    @Test
    @DisplayName("Aprobado: el JSON coincide campo a campo con el del enunciado")
    void serializar_conSolicitudAprobada_coincideConElJsonDelEnunciado() {
        String json = serializar(crearSolicitudConLaCadenaCompleta(EstadoSolicitud.APROBADO));

        assertThat((Object) serializador.readTree(json))
                .isEqualTo(serializador.readTree(JSON_ESPERADO_DEL_ENUNCIADO));
    }

    @Test
    @DisplayName("Preaprobado: trae las mismas secciones, con su tasa y su siguiente paso")
    void serializar_conSolicitudPreaprobada_traeTasaYSiguientePaso() {
        String json = serializar(crearSolicitudConLaCadenaCompleta(EstadoSolicitud.PREAPROBADO));

        assertThat(json)
                .contains("\"estado\":\"PREAPROBADO\"")
                .contains("\"tasaEstimada\":1.8")
                .contains(CAMPO_SIGUIENTE_PASO);
    }

    @Test
    @DisplayName("Rechazo temprano: el JSON no trae tasaEstimada ni siguientePaso")
    void serializar_conDocumentoBloqueado_noTraeTasaNiSiguientePaso() {
        String json = serializar(crearSolicitudRechazadaPorDocumentoBloqueado());

        assertThat(json)
                .doesNotContain(CAMPO_TASA)
                .doesNotContain(CAMPO_SIGUIENTE_PASO)
                .doesNotContain("scoreBureau");
    }

    @Test
    @DisplayName("Rechazo temprano: si trae el sobre, el solicitante, el detalle y la evaluacion")
    void serializar_conDocumentoBloqueado_traeElSobreYLasTresSeccionesQueSiAplican() {
        String json = serializar(crearSolicitudRechazadaPorDocumentoBloqueado());

        assertThat(json)
                .contains("\"estado\":\"RECHAZADO_FRAUDE\"")
                .contains("\"documento\":\"CC 1234567890\"")
                .contains("\"montoSolicitado\":15000000")
                .contains("\"resultado\":\"RECHAZADO\"");
    }

    private String serializar(Solicitud solicitud) {
        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);
        return serializador.writeValueAsString(respuesta);
    }
}
