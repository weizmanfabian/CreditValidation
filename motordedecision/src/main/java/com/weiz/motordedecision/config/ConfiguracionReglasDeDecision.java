package com.weiz.motordedecision.config;

import com.weiz.motordedecision.domain.decision.MotorDeDecision;
import com.weiz.motordedecision.domain.decision.ReglaAprobacionPorUmbrales;
import com.weiz.motordedecision.domain.decision.ReglaBuroNoDisponible;
import com.weiz.motordedecision.domain.decision.ReglaRechazoPorFraude;
import com.weiz.motordedecision.domain.decision.ReglaRechazoPorValidacion;
import com.weiz.motordedecision.domain.decision.ReglaRevisionManual;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Arma el motor de decision y le inyecta los umbrales de la tabla.
 *
 * Es la unica clase que sabe a la vez de Spring y de las reglas. Las reglas no
 * llevan {@code @Component}: si lo llevaran, el orden de evaluacion dependeria
 * del orden en que el contenedor las descubriera, y aqui el orden decide el
 * resultado (D-044). Es la lista literal de {@link #crearMotorDeDecision},
 * escrita en el orden de precedencia y no en otro.
 *
 * Anadir una fila a la tabla es escribir su clase y anadir una linea a esa
 * lista, siempre por delante de {@link ReglaRevisionManual}, que cierra.
 */
@Configuration
public class ConfiguracionReglasDeDecision {

    /**
     * Construye el motor con las filas de la tabla del enunciado, en orden de
     * precedencia.
     *
     * @param reglas umbrales de decision leidos de la configuracion
     * @return el motor listo para resolver estados
     */
    @Bean
    public MotorDeDecision crearMotorDeDecision(ReglasDecisionProperties reglas) {
        return new MotorDeDecision(List.of(
                new ReglaRechazoPorFraude(reglas.validacionDeIdentidad()),
                new ReglaBuroNoDisponible(),
                new ReglaRechazoPorValidacion(),
                crearReglaDeAprobacion(EstadoSolicitud.APROBADO, reglas.aprobado()),
                crearReglaDeAprobacion(EstadoSolicitud.PREAPROBADO, reglas.preaprobado()),
                new ReglaRevisionManual()));
    }

    private ReglaAprobacionPorUmbrales crearReglaDeAprobacion(EstadoSolicitud estado,
                                                              ReglasDecisionProperties.UmbralesDeEstado umbrales) {
        return new ReglaAprobacionPorUmbrales(estado, umbrales.scoreMinimo(), umbrales.multiploMaximoDeIngresos());
    }
}
