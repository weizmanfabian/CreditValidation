package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_BLOQUEADO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_LIMPIO;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el comportamiento de la validacion de identidad, que es la unica que no
 * necesita ningun insumo externo: solo la lista de bloqueados que recibe.
 */
@DisplayName("Validacion de identidad")
class ValidacionIdentidadTest {

    private final ValidacionIdentidad validacion =
            new ValidacionIdentidad(List.of(DOCUMENTO_BLOQUEADO, "1111111111"));

    @Test
    @DisplayName("Un documento que no esta en la lista pasa la validacion")
    void validar_conDocumentoFueraDeLaLista_devuelveVeredictoAprobado() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudImpecable();

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto)
                .returns(ResultadoEvaluacion.APROBADO, Veredicto::resultado)
                .returns("Documento no bloqueado", Veredicto::detalle);
    }

    @Test
    @DisplayName("Un documento de la lista se rechaza y el detalle dice por que")
    void validar_conDocumentoBloqueado_devuelveVeredictoRechazado() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudSinInformeDeBuro(DOCUMENTO_BLOQUEADO);

        Veredicto veredicto = validacion.validar(solicitud);

        assertThat(veredicto)
                .returns(ResultadoEvaluacion.RECHAZADO, Veredicto::resultado)
                .returns("Documento reportado en la lista de bloqueados", Veredicto::detalle);
    }

    @Test
    @DisplayName("No depende del buro: se pronuncia aunque el informe no exista")
    void puedeEvaluarse_sinInformeDelBuro_devuelveVerdadero() {
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudSinInformeDeBuro(DOCUMENTO_LIMPIO);

        boolean puedeEvaluarse = validacion.puedeEvaluarse(solicitud);

        assertThat(puedeEvaluarse).isTrue();
    }

    @Test
    @DisplayName("Su nombre es el que publica el enunciado en la respuesta")
    void obtenerNombre_siempre_devuelveElNombrePublicadoEnLaRespuesta() {
        String nombre = validacion.obtenerNombre();

        assertThat(nombre).isEqualTo("Identidad");
    }
}
