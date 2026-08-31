package com.weiz.buro.config;

import com.weiz.buro.domain.informe.GeneradorInformeCrediticio;
import com.weiz.buro.domain.informe.PausaBloqueante;
import com.weiz.buro.domain.informe.PausaDeSimulacion;
import com.weiz.buro.domain.informe.SimuladorConsultaBuro;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara como beans las piezas del buro simulado.
 *
 * El registro vive aqui y no en las propias clases: anotarlas con
 * {@code @Component} meteria una anotacion de Spring dentro de {@code domain} y
 * romperia la regla 2 de {@code docs/architecture.md} §3, que exige que el
 * dominio sea Java puro. Con {@code @Bean} el contenedor conoce al dominio, pero
 * el dominio no conoce al contenedor (decision D-019).
 */
@Configuration
@EnableConfigurationProperties(SimulacionBuroProperties.class)
public class ConfiguracionSimulacionBuro {

    /**
     * Registra el generador con su constructor sin argumentos, que usa el reloj
     * del sistema (D-017).
     */
    @Bean
    public GeneradorInformeCrediticio crearGeneradorInformeCrediticio() {
        return new GeneradorInformeCrediticio();
    }

    @Bean
    public PausaDeSimulacion crearPausaDeSimulacion() {
        return new PausaBloqueante();
    }

    @Bean
    public SimuladorConsultaBuro crearSimuladorConsultaBuro(GeneradorInformeCrediticio generadorInformeCrediticio,
                                                            PausaDeSimulacion pausaDeSimulacion,
                                                            SimulacionBuroProperties propiedades) {

        return new SimuladorConsultaBuro(generadorInformeCrediticio, pausaDeSimulacion,
                propiedades.documentoServicioCaido(), propiedades.retardoServicioCaido());
    }
}
