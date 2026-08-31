package com.weiz.motordedecision.config;

import com.weiz.motordedecision.mapper.AportanteDeDetalleFinanciero;
import com.weiz.motordedecision.mapper.AportanteDeEvaluacion;
import com.weiz.motordedecision.mapper.AportanteDeSeccion;
import com.weiz.motordedecision.mapper.AportanteDeSiguientePaso;
import com.weiz.motordedecision.mapper.AportanteDeSolicitante;
import com.weiz.motordedecision.mapper.MapeadorDeSolicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba lo que la configuracion aporta: que los cuatro aportantes esten
 * armados y que las tasas y los textos sean los que estan escritos en
 * application.yml, no los que invente un test.
 *
 * Aqui vive la comprobacion del rechazo temprano a nivel de dato:
 * {@code RECHAZADO_FRAUDE} no aparece en ninguno de los dos mapas, y esa
 * ausencia es la regla del enunciado (linea 91). Si alguien le anadiera una
 * tasa o un siguiente paso, este test se pondria rojo antes que el JSON.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Respuesta armada desde la configuracion")
class ConfiguracionDeRespuestaTest {

    private static final String CAMPO_DE_LOS_APORTANTES = "aportantes";

    @Autowired
    private MapeadorDeSolicitud mapeador;

    @Autowired
    private RespuestaProperties respuesta;

    @Test
    @DisplayName("El mapeador se arma con los aportantes de las cuatro secciones")
    void crearMapeadorDeSolicitud_desdeLaConfiguracion_armaLasCuatroSecciones() {
        @SuppressWarnings("unchecked") // El campo es una List<AportanteDeSeccion> declarada en el mapeador
        List<AportanteDeSeccion> aportantes =
                (List<AportanteDeSeccion>) ReflectionTestUtils.getField(mapeador, CAMPO_DE_LOS_APORTANTES);

        assertThat(aportantes).hasExactlyElementsOfTypes(
                AportanteDeSolicitante.class,
                AportanteDeDetalleFinanciero.class,
                AportanteDeEvaluacion.class,
                AportanteDeSiguientePaso.class);
    }

    @Test
    @DisplayName("Las tasas por estado son las del archivo: 1.2 para aprobado y 1.8 para preaprobado")
    void tasaEstimadaPorEstado_desdeElArchivo_traeSoloLosDosEstadosConOferta() {
        assertThat(respuesta.tasaEstimadaPorEstado())
                .containsEntry(EstadoSolicitud.APROBADO, new BigDecimal("1.2"))
                .containsEntry(EstadoSolicitud.PREAPROBADO, new BigDecimal("1.8"))
                .hasSize(2);
    }

    @Test
    @DisplayName("El siguiente paso de aprobado es el texto literal del enunciado")
    void siguientePasoPorEstado_desdeElArchivo_traeElTextoDelEnunciado() {
        assertThat(respuesta.siguientePasoPorEstado())
                .containsEntry(EstadoSolicitud.APROBADO, "Se enviará contrato al correo registrado en 24 horas")
                .hasSize(4);
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(value = EstadoSolicitud.class, names = "RECHAZADO_FRAUDE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Los otros cuatro estados si tienen siguiente paso")
    void siguientePasoPorEstado_enTodoEstadoSalvoElRechazoTemprano_traeTexto(EstadoSolicitud estado) {
        assertThat(respuesta.siguientePasoPorEstado()).containsKey(estado);
    }

    @Test
    @DisplayName("El rechazo temprano no tiene tasa ni siguiente paso en la configuracion")
    void configuracionDeRespuesta_paraRechazoPorFraude_noDeclaraTasaNiSiguientePaso() {
        assertThat(respuesta.tasaEstimadaPorEstado()).doesNotContainKey(EstadoSolicitud.RECHAZADO_FRAUDE);
        assertThat(respuesta.siguientePasoPorEstado()).doesNotContainKey(EstadoSolicitud.RECHAZADO_FRAUDE);
    }
}
