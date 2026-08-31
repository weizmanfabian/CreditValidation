package com.weiz.buro.config;

import com.weiz.buro.domain.informe.SimuladorConsultaBuro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija los valores con los que el buro arranca de verdad: los de
 * {@code application.yml}, no los de un perfil de prueba.
 *
 * Es lo que impide que el documento reservado para el servicio caido o el
 * retardo cambien sin que nadie se entere: el enunciado nombra {@code 0000000000}
 * de forma explicita, y el motor calibra su timeout contra ese retardo.
 */
@SpringBootTest
class SimulacionBuroPropertiesTest {

    @Autowired
    private SimulacionBuroProperties propiedades;

    @Autowired
    private ApplicationContext contexto;

    @Test
    @DisplayName("El documento que simula el servicio caido es el 0000000000 del enunciado")
    void enlazar_desdeApplicationYml_fijaElDocumentoDelEnunciado() {
        assertThat(propiedades.documentoServicioCaido()).isEqualTo("0000000000");
    }

    @Test
    @DisplayName("El retardo del servicio caido llega de la configuracion y supera el timeout del motor")
    void enlazar_desdeApplicationYml_fijaUnRetardoSuficienteParaAgotarElTimeout() {
        assertThat(propiedades.retardoServicioCaido()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("El simulador se registra como bean desde config, sin anotar el dominio")
    void cargarContexto_conLaConfiguracionDelModulo_registraElSimulador() {
        assertThat(contexto.getBeansOfType(SimuladorConsultaBuro.class)).hasSize(1);
    }
}
