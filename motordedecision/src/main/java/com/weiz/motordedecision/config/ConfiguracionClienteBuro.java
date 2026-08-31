package com.weiz.motordedecision.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Arma el cliente HTTP con el que el motor alcanza al buro.
 *
 * Se usa {@link RestClient}, el cliente sincrono de Spring Framework 7, y no
 * {@code RestTemplate}, que esta en mantenimiento (docs/architecture.md §5). La
 * fabrica de peticiones se construye a mano en {@link #crearFabricaConTiemposDeEspera}
 * porque los dos tiempos de espera son configuracion del modulo, no un valor
 * por omision del contenedor.
 */
@Configuration
public class ConfiguracionClienteBuro {

    /**
     * Registra el cliente HTTP del buro con su direccion base ya fijada, para
     * que quien lo use solo escriba la ruta del recurso.
     *
     * @param propiedades direccion y tiempos de espera del buro
     * @return cliente listo para consultar el buro
     */
    @Bean
    public RestClient crearClienteRestDelBuro(BuroProperties propiedades) {
        return RestClient.builder()
                .baseUrl(propiedades.url())
                .requestFactory(crearFabricaConTiemposDeEspera(propiedades))
                .build();
    }

    /**
     * Aplica los dos tiempos de espera, que cubren fallos distintos: el de
     * conexion ataja un buro que no esta escuchando; el de lectura, uno que
     * acepto la conexion y no contesta —el caso del enunciado, donde el buro
     * tarda unos diez segundos en vez de fallar.
     */
    private ClientHttpRequestFactory crearFabricaConTiemposDeEspera(BuroProperties propiedades) {
        HttpClient clienteJdk = HttpClient.newBuilder()
                .connectTimeout(propiedades.tiempoEsperaConexion())
                .build();

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(clienteJdk);
        fabrica.setReadTimeout(propiedades.tiempoEsperaLectura());
        return fabrica;
    }
}
