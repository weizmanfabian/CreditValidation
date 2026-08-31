package com.weiz.motordedecision.config;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.validacion.CadenaDeValidaciones;
import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.domain.validacion.SolicitudEvaluada;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba la cadena tal como queda armada en el contexto real, leyendo la
 * configuracion de application.yml y no valores inventados por el test.
 *
 * Es lo que separa "la lista de bloqueados vive en configuracion" de una
 * afirmacion sin comprobar: si alguien borrara el documento 1010101010 del
 * archivo, los datos de arranque de db/sql/02-data.sql dejarian de ser
 * coherentes y este test lo diria.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Cadena de validaciones armada desde la configuracion")
class ConfiguracionCadenaDeValidacionesTest {

    private static final String DOCUMENTO_BLOQUEADO_EN_LOS_DATOS_DE_ARRANQUE = "1010101010";
    private static final String DOCUMENTO_LIMPIO = "1234567890";

    @Autowired
    private CadenaDeValidaciones cadena;

    @Autowired
    private ReglasValidacionProperties reglas;

    @Test
    @DisplayName("La lista de bloqueados incluye el documento de los datos de arranque")
    void enlazar_alArrancar_incluyeElDocumentoBloqueadoDeLosDatosDeArranque() {
        assertThat(reglas.documentosBloqueados())
                .contains(DOCUMENTO_BLOQUEADO_EN_LOS_DATOS_DE_ARRANQUE)
                .hasSizeBetween(3, 5);
    }

    @Test
    @DisplayName("El orden lo fija la configuracion, no el escaneo: identidad primero, reporte negativo ultimo")
    void evaluarSolicitud_conElContextoReal_ejecutaLasCuatroValidacionesEnElOrdenDelEnunciado() {
        SolicitudEvaluada solicitud = crearSolicitudAprobable(DOCUMENTO_LIMPIO);

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(solicitud);

        assertThat(registros)
                .extracting(RegistroValidacion::nombre)
                .containsExactly("Identidad", "Score", "Capacidad de pago", "Reporte negativo");
    }

    @Test
    @DisplayName("El documento bloqueado de la configuracion detiene la cadena en la primera validacion")
    void evaluarSolicitud_conElDocumentoBloqueadoDeLaConfiguracion_seDetieneEnIdentidad() {
        SolicitudEvaluada solicitud = crearSolicitudAprobable(DOCUMENTO_BLOQUEADO_EN_LOS_DATOS_DE_ARRANQUE);

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(solicitud);

        assertThat(registros)
                .singleElement()
                .returns("Identidad", RegistroValidacion::nombre)
                .returns(ResultadoEvaluacion.RECHAZADO, RegistroValidacion::resultado);
    }

    private SolicitudEvaluada crearSolicitudAprobable(String numeroDocumento) {
        return new SolicitudEvaluada(numeroDocumento, new BigDecimal("15000000"), new BigDecimal("4000000"),
                ResultadoConsultaBuro.crearResultadoConInforme(750, "ACTIVO", false,
                        LocalDateTime.of(2026, 8, 24, 10, 30)));
    }
}
