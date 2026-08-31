package com.weiz.motordedecision.infraestructura.client;

import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.dto.buro.ConsultaBuroRequest;
import com.weiz.motordedecision.dto.buro.InformeCrediticioResponse;
import com.weiz.motordedecision.util.exceptions.tecnica.BuroNoDisponible;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Unico punto del motor que sabe que el buro se alcanza por HTTP.
 *
 * Alrededor de la llamada hay tres piezas de resiliencia, configuradas en
 * {@code application.yml} y aplicadas por Resilience4j (docs/conventions.md §7):
 * el reintento con backoff exponencial y jitter, el circuit breaker que deja de
 * insistir cuando el buro lleva un rato caido, y el fallback
 * {@link #crearResultadoDeBuroNoDisponible} que convierte el fallo en un valor.
 *
 * El detalle de como se combinan esta en {@link #consultarInforme}.
 */
@Component
public class BuroClientHttp implements BuroClient {

    private static final Logger log = LoggerFactory.getLogger(BuroClientHttp.class);

    /** Instancia de Resilience4j declarada en {@code application.yml}. */
    private static final String INSTANCIA_RESILIENCIA = "buro";

    private static final String RUTA_CONSULTA = "/api/buro/consulta";

    /** Digitos del documento que se dejan legibles en el log (docs/conventions.md §8). */
    private static final int DIGITOS_VISIBLES_DEL_DOCUMENTO = 4;

    private static final char CARACTER_DE_MASCARA = '*';

    private final RestClient clienteRestDelBuro;

    public BuroClientHttp(RestClient clienteRestDelBuro) {
        this.clienteRestDelBuro = clienteRestDelBuro;
    }

    /**
     * Consulta el informe crediticio del titular contra el buro.
     *
     * Las anotaciones se anidan en el orden por omision de Resilience4j: el
     * reintento envuelve al circuit breaker, asi que cada intento cuenta como
     * una llamada para el circuito. El fallback se declara en el reintento,
     * que es la capa de fuera, para que recoja cualquier fallo de las de
     * dentro: los reintentos agotados y tambien el circuito abierto.
     *
     * Ninguna excepcion de Spring sale de aqui: la del cliente HTTP se envuelve
     * en {@link BuroNoDisponible} (docs/conventions.md §6) y esa la absorbe el
     * fallback.
     *
     * @param tipoDocumento tipo de documento del titular
     * @param numeroDocumento numero de documento del titular
     * @return el informe del buro, o un resultado de buro no disponible
     */
    @Override
    @Retry(name = INSTANCIA_RESILIENCIA, fallbackMethod = "crearResultadoDeBuroNoDisponible")
    @CircuitBreaker(name = INSTANCIA_RESILIENCIA)
    public ResultadoConsultaBuro consultarInforme(String tipoDocumento, String numeroDocumento) {
        try {
            InformeCrediticioResponse informe = clienteRestDelBuro.post()
                    .uri(RUTA_CONSULTA)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ConsultaBuroRequest(tipoDocumento, numeroDocumento))
                    .retrieve()
                    .body(InformeCrediticioResponse.class);

            // Un 200 con el cuerpo vacio incumple el contrato del buro tanto como
            // un 500: se trata igual, para que el resultado nunca se lea nulo.
            if (informe == null) {
                throw new RestClientException("El buro respondio sin cuerpo");
            }

            return convertirAResultadoDeDominio(informe);

        } catch (RestClientException excepcion) {
            throw new BuroNoDisponible("Fallo la consulta al buro de credito", excepcion);
        }
    }

    /**
     * Fallback del reintento: traduce el buro caido en un valor.
     *
     * Recibe {@link Throwable} y no {@link BuroNoDisponible} a proposito, porque
     * tambien tiene que recoger la {@code CallNotPermittedException} que lanza
     * el circuit breaker cuando el circuito esta abierto y la llamada ni
     * siquiera sale.
     *
     * Se registra en {@code WARN} porque es una degradacion prevista, no un
     * fallo: la solicitud sigue adelante y terminara en
     * {@code PENDIENTE_REVISION} ({@code docs/error-handling.md} §4.4).
     */
    private ResultadoConsultaBuro crearResultadoDeBuroNoDisponible(String tipoDocumento,
                                                                   String numeroDocumento,
                                                                   Throwable causa) {

        String documentoEnmascarado = enmascararDocumento(numeroDocumento);
        Throwable fallo = obtenerCausaRaiz(causa);
        // El fallo no va de ultimo a proposito: SLF4J trata el ultimo argumento
        // como excepcion y volcaria la traza completa en un WARN que se repite
        // en cada consulta. Aqui basta con el tipo y el mensaje.
        log.warn("El buro no respondio ({}); el documento {} {} queda para revision manual",
                fallo, tipoDocumento, documentoEnmascarado);

        return ResultadoConsultaBuro.crearResultadoBuroNoDisponible();
    }

    /**
     * Devuelve el fallo de transporte que hay debajo de {@link BuroNoDisponible},
     * que es el que dice si el buro se cayo, tardo o devolvio un 5xx. Cuando la
     * causa no envuelve nada —el circuito abierto— se devuelve ella misma.
     */
    private static Throwable obtenerCausaRaiz(Throwable causa) {
        return causa.getCause() == null ? causa : causa.getCause();
    }

    private ResultadoConsultaBuro convertirAResultadoDeDominio(InformeCrediticioResponse informe) {
        return ResultadoConsultaBuro.crearResultadoConInforme(
                informe.score(), informe.estado(), informe.reporteNegativo(), informe.fechaConsulta());
    }

    /**
     * Deja visibles los primeros digitos y tapa el resto: el numero de documento
     * es un dato personal y no se escribe entero en el log.
     */
    private static String enmascararDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.length() <= DIGITOS_VISIBLES_DEL_DOCUMENTO) {
            return numeroDocumento;
        }

        int digitosTapados = numeroDocumento.length() - DIGITOS_VISIBLES_DEL_DOCUMENTO;
        return numeroDocumento.substring(0, DIGITOS_VISIBLES_DEL_DOCUMENTO)
                + String.valueOf(CARACTER_DE_MASCARA).repeat(digitosTapados);
    }
}
