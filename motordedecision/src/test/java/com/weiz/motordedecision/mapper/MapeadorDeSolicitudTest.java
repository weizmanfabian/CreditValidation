package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.response.SeccionDetalleFinanciero;
import com.weiz.motordedecision.api.models.response.SeccionEvaluacion;
import com.weiz.motordedecision.api.models.response.SeccionSolicitante;
import com.weiz.motordedecision.api.models.response.SolicitudCreditoResponse;
import com.weiz.motordedecision.api.models.response.ValidacionRealizada;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.DETALLE_DE_IDENTIDAD_BLOQUEADA;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.DOCUMENTO_CON_TIPO;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.FECHA_CREACION;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.ID_SOLICITUD;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.MONTO_SOLICITADO;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.NOMBRE_COMPLETO;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.PLAZO_MESES;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.SCORE;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.crearSolicitudConLaCadenaCompleta;
import static com.weiz.motordedecision.mapper.SolicitudesPersistidasDePrueba.crearSolicitudRechazadaPorDocumentoBloqueado;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba que secciones lleva la respuesta en cada estado, con los aportantes de
 * verdad pero con tasas y textos escritos aqui: lo que se comprueba es la
 * composicion, no los valores de application.yml —esos se prueban en
 * {@code ConfiguracionDeRespuestaTest}—.
 *
 * Es Java puro: el mapeador no depende de Spring.
 */
@DisplayName("Armado de la respuesta por secciones")
class MapeadorDeSolicitudTest {

    private static final BigDecimal TASA_DE_APROBADO = new BigDecimal("1.2");
    private static final BigDecimal TASA_DE_PREAPROBADO = new BigDecimal("1.8");
    private static final String PASO_DE_APROBADO = "Se enviará contrato al correo registrado en 24 horas";
    private static final String PASO_DE_PREAPROBADO = "Adjunte los documentos adicionales";

    private static final Map<EstadoSolicitud, BigDecimal> TASAS_POR_ESTADO = Map.of(
            EstadoSolicitud.APROBADO, TASA_DE_APROBADO,
            EstadoSolicitud.PREAPROBADO, TASA_DE_PREAPROBADO);
    private static final Map<EstadoSolicitud, String> PASOS_POR_ESTADO = Map.of(
            EstadoSolicitud.APROBADO, PASO_DE_APROBADO,
            EstadoSolicitud.PREAPROBADO, PASO_DE_PREAPROBADO);

    private final MapeadorDeSolicitud mapeador = new MapeadorDeSolicitud(crearAportantesReales());

    @Test
    @DisplayName("Aprobado: la respuesta trae el sobre y las cuatro secciones del enunciado")
    void mapearARespuesta_conSolicitudAprobada_traeTodasLasSecciones() {
        Solicitud solicitud = crearSolicitudConLaCadenaCompleta(EstadoSolicitud.APROBADO);

        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);

        assertThat(respuesta)
                .returns(ID_SOLICITUD, SolicitudCreditoResponse::idSolicitud)
                .returns(FECHA_CREACION, SolicitudCreditoResponse::fechaCreacion)
                .returns("APROBADO", SolicitudCreditoResponse::estado)
                .returns(PASO_DE_APROBADO, SolicitudCreditoResponse::siguientePaso);
        assertThat(respuesta.solicitante())
                .returns(NOMBRE_COMPLETO, SeccionSolicitante::nombre)
                .returns(DOCUMENTO_CON_TIPO, SeccionSolicitante::documento);
        assertThat(respuesta.detalle())
                .returns(MONTO_SOLICITADO, SeccionDetalleFinanciero::montoSolicitado)
                .returns(PLAZO_MESES, SeccionDetalleFinanciero::plazoMeses)
                .returns(TASA_DE_APROBADO, SeccionDetalleFinanciero::tasaEstimada);
        assertThat(respuesta.evaluacion().scoreBureau()).isEqualTo(SCORE);
    }

    @Test
    @DisplayName("Aprobado: la evaluacion repite el rastro de la cadena, en orden")
    void mapearARespuesta_conSolicitudAprobada_repiteElRastroDeLaCadena() {
        Solicitud solicitud = crearSolicitudConLaCadenaCompleta(EstadoSolicitud.APROBADO);

        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);

        assertThat(respuesta.evaluacion().validaciones())
                .extracting(ValidacionRealizada::nombre)
                .containsExactly("Identidad", "Score", "Capacidad de pago", "Reporte negativo");
    }

    @Test
    @DisplayName("Preaprobado: mismas secciones que aprobado, con su propia tasa y su propio texto")
    void mapearARespuesta_conSolicitudPreaprobada_traeSuTasaYSuSiguientePaso() {
        Solicitud solicitud = crearSolicitudConLaCadenaCompleta(EstadoSolicitud.PREAPROBADO);

        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);

        assertThat(respuesta)
                .returns("PREAPROBADO", SolicitudCreditoResponse::estado)
                .returns(PASO_DE_PREAPROBADO, SolicitudCreditoResponse::siguientePaso);
        assertThat(respuesta.detalle().tasaEstimada()).isEqualTo(TASA_DE_PREAPROBADO);
    }

    @Test
    @DisplayName("Rechazo temprano: sin tasa estimada y sin siguiente paso")
    void mapearARespuesta_conDocumentoBloqueado_noTraeTasaNiSiguientePaso() {
        Solicitud solicitud = crearSolicitudRechazadaPorDocumentoBloqueado();

        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);

        assertThat(respuesta)
                .returns("RECHAZADO_FRAUDE", SolicitudCreditoResponse::estado)
                .returns(null, SolicitudCreditoResponse::siguientePaso);
        assertThat(respuesta.detalle().tasaEstimada()).isNull();
    }

    @Test
    @DisplayName("Rechazo temprano: conserva el motivo y no inventa score")
    void mapearARespuesta_conDocumentoBloqueado_conservaElMotivoYNoTraeScore() {
        Solicitud solicitud = crearSolicitudRechazadaPorDocumentoBloqueado();

        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);

        assertThat(respuesta.evaluacion())
                .returns(null, SeccionEvaluacion::scoreBureau)
                .returns(List.of(new ValidacionRealizada("Identidad", "RECHAZADO", DETALLE_DE_IDENTIDAD_BLOQUEADA)),
                        SeccionEvaluacion::validaciones);
    }

    @ParameterizedTest(name = "estado {0}")
    @EnumSource(EstadoSolicitud.class)
    @DisplayName("En los cinco estados la respuesta trae siempre solicitante, detalle y evaluacion")
    void mapearARespuesta_enCualquierEstado_traeLasTresSeccionesQueNoDependenDelEstado(EstadoSolicitud estado) {
        Solicitud solicitud = crearSolicitudConLaCadenaCompleta(estado);

        SolicitudCreditoResponse respuesta = mapeador.mapearARespuesta(solicitud);

        assertThat(respuesta)
                .doesNotReturn(null, SolicitudCreditoResponse::solicitante)
                .doesNotReturn(null, SolicitudCreditoResponse::detalle)
                .doesNotReturn(null, SolicitudCreditoResponse::evaluacion);
    }

    @Test
    @DisplayName("Una seccion nueva se agrega sin tocar las cuatro existentes")
    void mapearARespuesta_conUnAportanteMas_conservaLasCuatroSeccionesYAgregaLaNueva() {
        Solicitud solicitud = crearSolicitudConLaCadenaCompleta(EstadoSolicitud.APROBADO);
        SolicitudCreditoResponse respuestaSinLaSeccionNueva = mapeador.mapearARespuesta(solicitud);
        List<EstadoSolicitud> loQueRecibeLaSeccionNueva = new ArrayList<>();
        List<AportanteDeSeccion> conUnaSeccionMas = new ArrayList<>(crearAportantesReales());
        conUnaSeccionMas.add((constructor, solicitudAportada, estado) -> loQueRecibeLaSeccionNueva.add(estado));

        SolicitudCreditoResponse respuesta = new MapeadorDeSolicitud(conUnaSeccionMas).mapearARespuesta(solicitud);

        // Las cuatro secciones existentes siguen en pie, con los mismos valores que sin el aportante nuevo
        assertThat(respuesta)
                .isEqualTo(respuestaSinLaSeccionNueva)
                .returns(NOMBRE_COMPLETO, unaRespuesta -> unaRespuesta.solicitante().nombre())
                .returns(TASA_DE_APROBADO, unaRespuesta -> unaRespuesta.detalle().tasaEstimada())
                .returns(SCORE, unaRespuesta -> unaRespuesta.evaluacion().scoreBureau())
                .returns(PASO_DE_APROBADO, SolicitudCreditoResponse::siguientePaso);
        // Y la seccion nueva recibio su turno, con el estado que decidio el motor
        assertThat(loQueRecibeLaSeccionNueva).containsExactly(EstadoSolicitud.APROBADO);
    }

    private static List<AportanteDeSeccion> crearAportantesReales() {
        return List.of(
                new AportanteDeSolicitante(),
                new AportanteDeDetalleFinanciero(TASAS_POR_ESTADO),
                new AportanteDeEvaluacion(),
                new AportanteDeSiguientePaso(PASOS_POR_ESTADO));
    }
}
