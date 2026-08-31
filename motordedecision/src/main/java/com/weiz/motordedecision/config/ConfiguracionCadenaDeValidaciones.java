package com.weiz.motordedecision.config;

import com.weiz.motordedecision.domain.validacion.CadenaDeValidaciones;
import com.weiz.motordedecision.domain.validacion.ValidacionCapacidadDePago;
import com.weiz.motordedecision.domain.validacion.ValidacionIdentidad;
import com.weiz.motordedecision.domain.validacion.ValidacionReporteNegativo;
import com.weiz.motordedecision.domain.validacion.ValidacionScoreCrediticio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Arma la cadena de validaciones y le inyecta su configuracion.
 *
 * Es la unica clase que sabe a la vez de Spring y de las validaciones. Las
 * validaciones no llevan {@code @Component}: si lo llevaran, el orden de la
 * cadena dependeria del orden en que el contenedor las descubriera, que no es
 * un contrato del que se pueda depender. Aqui el orden es la lista literal de
 * {@link #crearCadenaDeValidaciones}, en el mismo orden que fija el enunciado.
 *
 * Agregar una quinta validacion es escribir su clase y anadir una linea a esa
 * lista; ni la cadena ni las cuatro existentes se tocan.
 */
@Configuration
public class ConfiguracionCadenaDeValidaciones {

    /**
     * Construye la cadena con las cuatro validaciones del enunciado, en orden.
     *
     * @param reglas umbrales y lista de bloqueados leidos de la configuracion
     * @return la cadena lista para evaluar solicitudes
     */
    @Bean
    public CadenaDeValidaciones crearCadenaDeValidaciones(ReglasValidacionProperties reglas) {
        return new CadenaDeValidaciones(List.of(
                new ValidacionIdentidad(reglas.documentosBloqueados()),
                new ValidacionScoreCrediticio(reglas.scoreMinimo()),
                new ValidacionCapacidadDePago(reglas.multiploMaximoDeIngresos()),
                new ValidacionReporteNegativo()));
    }
}
