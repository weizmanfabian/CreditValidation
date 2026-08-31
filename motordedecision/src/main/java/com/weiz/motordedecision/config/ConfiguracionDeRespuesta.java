package com.weiz.motordedecision.config;

import com.weiz.motordedecision.mapper.AportanteDeDetalleFinanciero;
import com.weiz.motordedecision.mapper.AportanteDeEvaluacion;
import com.weiz.motordedecision.mapper.AportanteDeSiguientePaso;
import com.weiz.motordedecision.mapper.AportanteDeSolicitante;
import com.weiz.motordedecision.mapper.MapeadorDeSolicitud;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Arma el mapeador de la respuesta con sus cuatro secciones.
 *
 * Sigue el mismo patron que la cadena de validaciones (D-040) y las reglas de
 * decision (D-044): la lista es literal y esta escrita aqui, no la descubre el
 * escaneo de componentes. La razon aqui no es el orden —cada aportante escribe
 * su propia seccion y no depende de las demas— sino que dos de los cuatro
 * necesitan configuracion por constructor, que un {@code @Component} no sabe
 * darles sin ensuciarlos con anotaciones.
 *
 * Agregar una seccion es escribir su aportante y anadir una linea a esta lista.
 */
@Configuration
public class ConfiguracionDeRespuesta {

    /**
     * Construye el mapeador con los aportantes de las cuatro secciones del
     * enunciado.
     *
     * @param respuesta tasas y textos por estado, leidos de la configuracion
     * @return el mapeador listo para armar respuestas
     */
    @Bean
    public MapeadorDeSolicitud crearMapeadorDeSolicitud(RespuestaProperties respuesta) {
        return new MapeadorDeSolicitud(List.of(
                new AportanteDeSolicitante(),
                new AportanteDeDetalleFinanciero(respuesta.tasaEstimadaPorEstado()),
                new AportanteDeEvaluacion(),
                new AportanteDeSiguientePaso(respuesta.siguientePasoPorEstado())));
    }
}
