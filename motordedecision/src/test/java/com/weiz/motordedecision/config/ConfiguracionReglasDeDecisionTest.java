package com.weiz.motordedecision.config;

import com.weiz.motordedecision.domain.decision.CasoDeDecision;
import com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba;
import com.weiz.motordedecision.domain.decision.MotorDeDecision;
import com.weiz.motordedecision.domain.decision.ReglaAprobacionPorUmbrales;
import com.weiz.motordedecision.domain.decision.ReglaBuroNoDisponible;
import com.weiz.motordedecision.domain.decision.ReglaRechazoPorFraude;
import com.weiz.motordedecision.domain.decision.ReglaRechazoPorValidacion;
import com.weiz.motordedecision.domain.decision.ReglaRevisionManual;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.IDENTIDAD;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_CINCO_VECES_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_CINCO_VECES_LOS_INGRESOS_MAS_UNO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_OCHO_VECES_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_OCHO_VECES_LOS_INGRESOS_MAS_UNO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_SIETE_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.REPORTE_NEGATIVO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_ALTO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_INTERMEDIO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_JUSTO_POR_DEBAJO_DE_APROBADO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_MINIMO_DE_APROBADO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.SCORE_MINIMO_DE_PREAPROBADO;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoConLaCadenaCompleta;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoRechazadoEn;
import static com.weiz.motordedecision.domain.decision.CasosDeDecisionDePrueba.crearCasoSinInformeDeBuro;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba la tabla de decision del enunciado (lineas 78-84) contra el motor
 * real: el que arma {@link ConfiguracionReglasDeDecision} con sus seis reglas y
 * con los umbrales que estan escritos en application.yml, no con numeros que
 * invente el test.
 *
 * Es la diferencia entre comprobar los cortes y afirmarlos. Si alguien cambiara
 * el 700 del archivo, o pusiera la fila de PREAPROBADO por delante de la de
 * APROBADO, o cambiara un {@code >=} por un {@code >}, alguna fila de
 * {@link #generarFilasDeLaTablaDeDecision} se pondria roja.
 *
 * El recorrido del motor —que decide la primera regla que aplica— se prueba
 * aparte, con reglas de mentira, en {@code MotorDeDecisionTest}.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Tabla de decision armada desde la configuracion")
class ConfiguracionReglasDeDecisionTest {

    private static final String CAMPO_DE_LAS_REGLAS = "reglas";
    private static final int MULTIPLO_MAXIMO_DE_APROBADO = 8;
    private static final int MULTIPLO_MAXIMO_DE_PREAPROBADO = 5;

    @Autowired
    private MotorDeDecision motor;

    @Autowired
    private ReglasDecisionProperties reglas;

    /**
     * Un caso por fila de la tabla del enunciado, mas los limites exactos de
     * los cuatro umbrales: el score justo y el score justo por debajo, el monto
     * justo y el monto justo por encima.
     *
     * La fila del monto de ocho veces los ingresos mas un peso con la cadena
     * completa no puede darse en produccion —la validacion de capacidad de pago
     * corta antes en ese mismo multiplo (D-042)—, y esta a proposito: fija que
     * si algun dia dejara de cortar, la decision tampoco aprobaria.
     *
     * @return escenario, caso y estado que la tabla le asigna
     */
    static Stream<Arguments> generarFilasDeLaTablaDeDecision() {
        return Stream.of(
                Arguments.of("Documento en lista bloqueada",
                        crearCasoRechazadoEn(IDENTIDAD), EstadoSolicitud.RECHAZADO_FRAUDE),
                Arguments.of("Documento bloqueado con el buro caido: el fraude manda sobre la revision",
                        crearCasoSinInformeDeBuro(ResultadoEvaluacion.RECHAZADO), EstadoSolicitud.RECHAZADO_FRAUDE),
                Arguments.of("Servicio de buro no disponible",
                        crearCasoSinInformeDeBuro(ResultadoEvaluacion.APROBADO), EstadoSolicitud.PENDIENTE_REVISION),
                Arguments.of("Falla la validacion de score",
                        crearCasoRechazadoEn(SCORE), EstadoSolicitud.RECHAZADO),
                Arguments.of("Falla la validacion de reporte negativo",
                        crearCasoRechazadoEn(REPORTE_NEGATIVO), EstadoSolicitud.RECHAZADO),
                Arguments.of("Score 750 y monto muy por debajo del tope: cumple las dos filas y gana APROBADO",
                        crearCasoConLaCadenaCompleta(SCORE_ALTO, MONTO_DE_TRES_VECES_Y_MEDIA_LOS_INGRESOS),
                        EstadoSolicitud.APROBADO),
                Arguments.of("Score 750 y monto de siete veces y media los ingresos",
                        crearCasoConLaCadenaCompleta(SCORE_ALTO, MONTO_DE_SIETE_VECES_Y_MEDIA_LOS_INGRESOS),
                        EstadoSolicitud.APROBADO),
                Arguments.of("Score 700 justo y monto de ocho veces los ingresos justo: el borde aprueba",
                        crearCasoConLaCadenaCompleta(SCORE_MINIMO_DE_APROBADO, MONTO_DE_OCHO_VECES_LOS_INGRESOS),
                        EstadoSolicitud.APROBADO),
                Arguments.of("Score 699 con el mismo monto: un punto por debajo ya no aprueba",
                        crearCasoConLaCadenaCompleta(SCORE_JUSTO_POR_DEBAJO_DE_APROBADO,
                                MONTO_DE_OCHO_VECES_LOS_INGRESOS),
                        EstadoSolicitud.PENDIENTE_REVISION),
                Arguments.of("Score 700 con un peso mas de ocho veces los ingresos: el monto ya no cabe",
                        crearCasoConLaCadenaCompleta(SCORE_MINIMO_DE_APROBADO,
                                MONTO_DE_OCHO_VECES_LOS_INGRESOS_MAS_UNO),
                        EstadoSolicitud.PENDIENTE_REVISION),
                Arguments.of("Score 640 y monto de cinco veces los ingresos justo",
                        crearCasoConLaCadenaCompleta(SCORE_INTERMEDIO, MONTO_DE_CINCO_VECES_LOS_INGRESOS),
                        EstadoSolicitud.PREAPROBADO),
                Arguments.of("Score 600 justo y monto de cinco veces los ingresos: el borde preaprueba",
                        crearCasoConLaCadenaCompleta(SCORE_MINIMO_DE_PREAPROBADO, MONTO_DE_CINCO_VECES_LOS_INGRESOS),
                        EstadoSolicitud.PREAPROBADO),
                Arguments.of("Score 640 con un peso mas de cinco veces los ingresos: el hueco de la tabla",
                        crearCasoConLaCadenaCompleta(SCORE_INTERMEDIO, MONTO_DE_CINCO_VECES_LOS_INGRESOS_MAS_UNO),
                        EstadoSolicitud.PENDIENTE_REVISION));
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("generarFilasDeLaTablaDeDecision")
    @DisplayName("Cada fila de la tabla del enunciado decide el estado que le corresponde")
    void decidirEstado_conCadaFilaDeLaTabla_devuelveElEstadoDelEnunciado(String escenario, CasoDeDecision caso,
                                                                        EstadoSolicitud estadoEsperado) {
        EstadoSolicitud estado = motor.decidirEstado(caso);

        assertThat(estado).as(escenario).isEqualTo(estadoEsperado);
    }

    @Test
    @DisplayName("Los umbrales salen de reglas.decision: 700 con ocho veces, 600 con cinco")
    void enlazar_alArrancar_leeLosCuatroUmbralesDeLaTablaDesdeLaConfiguracion() {
        assertThat(reglas.validacionDeIdentidad()).isEqualTo(IDENTIDAD);
        assertThat(reglas.aprobado())
                .returns(SCORE_MINIMO_DE_APROBADO, ReglasDecisionProperties.UmbralesDeEstado::scoreMinimo)
                .returns(MULTIPLO_MAXIMO_DE_APROBADO,
                        ReglasDecisionProperties.UmbralesDeEstado::multiploMaximoDeIngresos);
        assertThat(reglas.preaprobado())
                .returns(SCORE_MINIMO_DE_PREAPROBADO, ReglasDecisionProperties.UmbralesDeEstado::scoreMinimo)
                .returns(MULTIPLO_MAXIMO_DE_PREAPROBADO,
                        ReglasDecisionProperties.UmbralesDeEstado::multiploMaximoDeIngresos);
    }

    @Test
    @DisplayName("El bean se arma con las seis reglas en el orden de precedencia, y la de revision manual cierra")
    void crearMotorDeDecision_alArrancar_armaLasSeisReglasEnElOrdenDePrecedencia() {
        // El orden es contrato (D-044) y el motor no lo publica: se lee la lista
        // que recibio por constructor, que es lo que fija ConfiguracionReglasDeDecision.
        List<?> reglasDelMotor = (List<?>) ReflectionTestUtils.getField(motor, CAMPO_DE_LAS_REGLAS);

        assertThat(reglasDelMotor)
                .extracting(Object::getClass)
                .containsExactly(
                        ReglaRechazoPorFraude.class,
                        ReglaBuroNoDisponible.class,
                        ReglaRechazoPorValidacion.class,
                        ReglaAprobacionPorUmbrales.class,
                        ReglaAprobacionPorUmbrales.class,
                        ReglaRevisionManual.class);
    }
}
