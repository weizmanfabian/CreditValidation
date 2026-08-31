package com.weiz.motordedecision.domain.validacion;

import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_BLOQUEADO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.DOCUMENTO_LIMPIO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.INGRESOS;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.MONTO;
import static com.weiz.motordedecision.domain.validacion.SolicitudesDePrueba.SCORE_ALTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Fija las dos propiedades que hacen de esto una cadena y no una lista de
 * llamadas: se ejecuta en el orden recibido y se detiene en la primera
 * validacion que rechaza.
 *
 * El corte no se comprueba solo contando registros —eso lo cumpliria una
 * implementacion que evaluara todo y descartara el sobrante—: las validaciones
 * de prueba cuentan sus invocaciones, asi que el test falla si la cadena
 * llegara a ejecutarlas.
 */
@DisplayName("Cadena de validaciones")
class CadenaDeValidacionesTest {

    private static final int SCORE_MINIMO = 600;
    private static final int MULTIPLO_MAXIMO = 8;

    private final ValidacionContadora testigo = new ValidacionContadora("Testigo");

    @Test
    @DisplayName("Una solicitud impecable recorre las cuatro validaciones, numeradas de 1 a 4")
    void evaluarSolicitud_conSolicitudImpecable_registraLasCuatroValidacionesEnOrden() {
        CadenaDeValidaciones cadena = crearCadenaCompleta();

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(SolicitudesDePrueba.crearSolicitudImpecable());

        assertThat(registros)
                .extracting(RegistroValidacion::orden, RegistroValidacion::nombre, RegistroValidacion::resultado)
                .containsExactly(
                        tuple((short) 1, "Identidad", ResultadoEvaluacion.APROBADO),
                        tuple((short) 2, "Score", ResultadoEvaluacion.APROBADO),
                        tuple((short) 3, "Capacidad de pago", ResultadoEvaluacion.APROBADO),
                        tuple((short) 4, "Reporte negativo", ResultadoEvaluacion.APROBADO));
    }

    @Test
    @DisplayName("Un documento bloqueado detiene la cadena en la primera validacion")
    void evaluarSolicitud_conDocumentoBloqueado_seDetieneEnLaPrimeraValidacion() {
        CadenaDeValidaciones cadena = new CadenaDeValidaciones(List.of(
                new ValidacionIdentidad(List.of(DOCUMENTO_BLOQUEADO)),
                testigo));
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudConInforme(
                DOCUMENTO_BLOQUEADO, MONTO, INGRESOS, SCORE_ALTO, false);

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(solicitud);

        assertThat(registros)
                .singleElement()
                .returns("Identidad", RegistroValidacion::nombre)
                .returns(ResultadoEvaluacion.RECHAZADO, RegistroValidacion::resultado);
        assertThat(testigo.contarInvocaciones()).isZero();
    }

    @Test
    @DisplayName("Un score insuficiente corta despues de identidad: no se evalua lo que viene detras")
    void evaluarSolicitud_conScoreInsuficiente_noEjecutaLasValidacionesSiguientes() {
        CadenaDeValidaciones cadena = new CadenaDeValidaciones(List.of(
                new ValidacionIdentidad(List.of(DOCUMENTO_BLOQUEADO)),
                new ValidacionScoreCrediticio(SCORE_MINIMO),
                testigo));
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudConInforme(
                DOCUMENTO_LIMPIO, MONTO, INGRESOS, 420, true);

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(solicitud);

        assertThat(registros)
                .extracting(RegistroValidacion::nombre, RegistroValidacion::resultado)
                .containsExactly(
                        tuple("Identidad", ResultadoEvaluacion.APROBADO),
                        tuple("Score", ResultadoEvaluacion.RECHAZADO));
        assertThat(testigo.contarInvocaciones()).isZero();
    }

    @Test
    @DisplayName("Sin informe del buro la cadena para antes del score y no inventa un rechazo")
    void evaluarSolicitud_sinInformeDelBuro_seDetieneSinRegistrarLaValidacionQueLoNecesita() {
        CadenaDeValidaciones cadena = crearCadenaCompleta();
        SolicitudEvaluada solicitud = SolicitudesDePrueba.crearSolicitudSinInformeDeBuro(DOCUMENTO_LIMPIO);

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(solicitud);

        assertThat(registros)
                .singleElement()
                .returns("Identidad", RegistroValidacion::nombre)
                .returns(ResultadoEvaluacion.APROBADO, RegistroValidacion::resultado);
    }

    @Test
    @DisplayName("Una quinta validacion entra en la cadena sin tocar las cuatro existentes")
    void evaluarSolicitud_conUnaQuintaValidacion_laEjecutaSinModificarLasExistentes() {
        ValidacionContadora quinta = new ValidacionContadora("Antiguedad laboral");
        CadenaDeValidaciones cadena = new CadenaDeValidaciones(List.of(
                new ValidacionIdentidad(List.of(DOCUMENTO_BLOQUEADO)),
                new ValidacionScoreCrediticio(SCORE_MINIMO),
                new ValidacionCapacidadDePago(MULTIPLO_MAXIMO),
                new ValidacionReporteNegativo(),
                quinta));

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(SolicitudesDePrueba.crearSolicitudImpecable());

        assertThat(registros)
                .hasSize(5)
                .last()
                .returns((short) 5, RegistroValidacion::orden)
                .returns("Antiguedad laboral", RegistroValidacion::nombre);
    }

    @Test
    @DisplayName("La cadena conserva el orden que recibe, no el que le convenga")
    void evaluarSolicitud_conLaCadenaAlReves_registraLasValidacionesEnEseMismoOrden() {
        CadenaDeValidaciones cadena = new CadenaDeValidaciones(List.of(
                new ValidacionReporteNegativo(),
                new ValidacionIdentidad(List.of(DOCUMENTO_BLOQUEADO))));

        List<RegistroValidacion> registros = cadena.evaluarSolicitud(SolicitudesDePrueba.crearSolicitudImpecable());

        assertThat(registros)
                .extracting(RegistroValidacion::nombre)
                .containsExactly("Reporte negativo", "Identidad");
    }

    private CadenaDeValidaciones crearCadenaCompleta() {
        return new CadenaDeValidaciones(List.of(
                new ValidacionIdentidad(List.of(DOCUMENTO_BLOQUEADO)),
                new ValidacionScoreCrediticio(SCORE_MINIMO),
                new ValidacionCapacidadDePago(MULTIPLO_MAXIMO),
                new ValidacionReporteNegativo()));
    }

    /**
     * Validacion de prueba que siempre aprueba y cuenta cuantas veces la
     * llamaron. Es lo que convierte "no aparece en los registros" en "no se
     * ejecuto", que es lo que el criterio de corte temprano exige.
     */
    private static final class ValidacionContadora implements ValidacionCrediticia {

        private final String nombre;
        private int invocaciones;

        private ValidacionContadora(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public String obtenerNombre() {
            return nombre;
        }

        @Override
        public Veredicto validar(SolicitudEvaluada solicitud) {
            invocaciones++;
            return Veredicto.crearVeredictoAprobado("Validacion de prueba");
        }

        private int contarInvocaciones() {
            return invocaciones;
        }
    }
}
