package com.weiz.buro.domain.informe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Prueba el adaptador que ejecuta de verdad la pausa: es donde vive el
 * comportamiento que el buro simula, y por tanto lo que no puede quedar sin
 * fijar (hallazgo M1 de {@code progress/review_buro_endpoint.md}).
 *
 * Ningun caso mide cuanto tarda: en vez de cronometrar la espera —la asercion
 * intermitente que {@code docs/verification.md} §3 prohibe— se observa el
 * estado del hilo. Un hilo que ejecuta la pausa entra en {@code TIMED_WAITING};
 * si {@code pausar} no bloqueara, terminaria de inmediato y los dos casos
 * principales fallarian. Cada caso dura milisegundos: la espera de cinco
 * minutos nunca se consuma, se interrumpe en cuanto el hilo queda bloqueado.
 */
class PausaBloqueanteTest {

    private static final Duration RETARDO_LARGO = Duration.ofMinutes(5);
    private static final String NOMBRE_DEL_HILO = "pausa-de-prueba";

    private static final int SONDEOS_MAXIMOS = 500;
    private static final long MILISEGUNDOS_ENTRE_SONDEOS = 2;
    private static final long SEGUNDOS_DE_ESPERA_MAXIMA = 5;

    private final PausaBloqueante pausa = new PausaBloqueante();
    private final List<Thread> hilosLanzados = new ArrayList<>();

    /**
     * Ningun hilo de un caso sobrevive al siguiente, y la marca de interrupcion
     * del hilo de JUnit se limpia para no contaminar el resto de la suite.
     */
    @AfterEach
    void detenerLosHilosLanzados() throws InterruptedException {
        for (Thread hilo : hilosLanzados) {
            hilo.interrupt();
            hilo.join(TimeUnit.SECONDS.toMillis(SEGUNDOS_DE_ESPERA_MAXIMA));
        }
        hilosLanzados.clear();
        Thread.interrupted();
    }

    @Test
    @DisplayName("La pausa bloquea el hilo que atiende la consulta durante la duracion pedida")
    void pausar_conUnaDuracionLarga_bloqueaElHiloQueLaEjecuta() throws InterruptedException {
        PausaEnOtroHilo pausaEnCurso = lanzarPausaEnOtroHilo(RETARDO_LARGO);

        Thread.State estadoDurantePausa = observarEstadoHastaQueSeBloqueeOTermine(pausaEnCurso.hilo());

        assertThat(estadoDurantePausa).isEqualTo(Thread.State.TIMED_WAITING);
    }

    @Test
    @DisplayName("Interrumpir el hilo bloqueado termina la pausa y conserva la marca de interrupcion")
    void pausar_alInterrumpirElHiloBloqueado_terminaYRestauraLaMarcaDeInterrupcion() throws InterruptedException {
        PausaEnOtroHilo pausaEnCurso = lanzarPausaEnOtroHilo(RETARDO_LARGO);
        assertThat(observarEstadoHastaQueSeBloqueeOTermine(pausaEnCurso.hilo()))
                .as("el hilo debe estar bloqueado antes de interrumpirlo, o el caso no probaria nada")
                .isEqualTo(Thread.State.TIMED_WAITING);

        pausaEnCurso.hilo().interrupt();

        assertThat(pausaEnCurso.pausaTerminada().await(SEGUNDOS_DE_ESPERA_MAXIMA, TimeUnit.SECONDS))
                .as("la pausa debe terminar al interrumpir el hilo, sin agotar los cinco minutos")
                .isTrue();
        assertThat(pausaEnCurso.marcaDeInterrupcionAlTerminar()).isTrue();
    }

    /**
     * De las tres filas, las que fijan la guarda son la nula y la negativa: sin
     * el {@code if}, {@code Thread.sleep} las rechazaria con
     * {@code IllegalArgumentException} y la consulta acabaria en 500. La fila
     * {@code PT0S} documenta la intencion, pero pasaria igual sin la guarda.
     */
    @ParameterizedTest(name = "una pausa de {0} no interrumpe la consulta")
    @ValueSource(strings = {"PT0S", "PT-1S"})
    @DisplayName("Una duracion nula o negativa se ignora en vez de romper la consulta")
    void pausar_conDuracionNulaONegativa_noLanzaExcepcion(Duration duracion) {
        assertThatCode(() -> pausa.pausar(duracion)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Sin duracion no hay pausa que aplicar y la consulta sigue su curso")
    void pausar_sinDuracion_noLanzaExcepcion() {
        assertThatCode(() -> pausa.pausar(null)).doesNotThrowAnyException();
    }

    /**
     * Ejecuta la pausa en un hilo aparte y anota, cuando termina, si conservo la
     * marca de interrupcion.
     */
    private PausaEnOtroHilo lanzarPausaEnOtroHilo(Duration duracion) {
        CountDownLatch pausaTerminada = new CountDownLatch(1);
        AtomicBoolean marcaDeInterrupcion = new AtomicBoolean();

        Thread hilo = new Thread(() -> {
            pausa.pausar(duracion);
            marcaDeInterrupcion.set(Thread.currentThread().isInterrupted());
            pausaTerminada.countDown();
        }, NOMBRE_DEL_HILO);

        hilosLanzados.add(hilo);
        hilo.start();

        return new PausaEnOtroHilo(hilo, pausaTerminada, marcaDeInterrupcion);
    }

    /**
     * Sondea el estado del hilo hasta que quede bloqueado en la pausa o termine.
     *
     * El tope de sondeos existe para que un fallo no cuelgue la suite, no como
     * asercion: lo que se afirma es el estado observado, no cuanto tardo en
     * observarse.
     *
     * @param hilo hilo que esta ejecutando la pausa
     * @return {@code TIMED_WAITING} si la pausa bloquea, {@code TERMINATED} si no
     */
    private static Thread.State observarEstadoHastaQueSeBloqueeOTermine(Thread hilo) throws InterruptedException {
        for (int sondeo = 0; sondeo < SONDEOS_MAXIMOS; sondeo++) {
            Thread.State estado = hilo.getState();
            if (estado == Thread.State.TIMED_WAITING || estado == Thread.State.TERMINATED) {
                return estado;
            }
            Thread.sleep(MILISEGUNDOS_ENTRE_SONDEOS);
        }

        return hilo.getState();
    }

    /**
     * Pausa lanzada en un hilo aparte, con lo necesario para observarla.
     *
     * @param hilo hilo que ejecuta la pausa
     * @param pausaTerminada se abre cuando la pausa devuelve el control
     * @param marcaDeInterrupcionAlTerminar marca de interrupcion del hilo al terminar la pausa
     */
    private record PausaEnOtroHilo(Thread hilo,
                                   CountDownLatch pausaTerminada,
                                   AtomicBoolean marcaDeInterrupcionAlTerminar) {
    }
}
