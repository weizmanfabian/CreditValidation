package com.weiz.buro.domain.informe;

import com.weiz.buro.api.models.response.InformeCrediticioResponse;
import com.weiz.buro.util.enums.EstadoTitular;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Aplica las reglas del buro simulado que fija el enunciado: si el numero de
 * documento termina en digito par el titular esta al dia (score en
 * [600, 850], sin reporte negativo); si termina en impar arrastra reporte
 * negativo (score en [300, 550]).
 *
 * Es Java puro, sin Spring ni anotaciones de framework, y no guarda estado
 * mutable: cada informe se calcula a partir del documento consultado.
 *
 * El score es determinista. La semilla se deriva del propio numero de
 * documento, asi que el mismo documento devuelve siempre el mismo score y las
 * pruebas y la demostracion manual son reproducibles (decision D-003).
 */
public final class GeneradorInformeCrediticio {

    private static final int SCORE_MINIMO_SIN_REPORTE = 600;
    private static final int SCORE_MAXIMO_SIN_REPORTE = 850;
    private static final int SCORE_MINIMO_CON_REPORTE = 300;
    private static final int SCORE_MAXIMO_CON_REPORTE = 550;

    private static final int BASE_DECIMAL = 10;
    private static final int DIVISOR_PARIDAD = 2;

    /**
     * Constantes del hash FNV-1a de 64 bits, la funcion no criptografica que
     * dispersa la semilla. Se usa por tres motivos: esta especificada digito a
     * digito (mismo resultado en cualquier JVM), no arrastra estado y
     * documentos consecutivos no producen scores consecutivos.
     */
    private static final long SEMILLA_BASE_FNV = 0xcbf29ce484222325L;
    private static final long FACTOR_FNV = 0x100000001b3L;

    private final Clock reloj;

    /**
     * Crea el generador con el reloj del sistema.
     */
    public GeneradorInformeCrediticio() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Crea el generador con un reloj explicito, para que {@code fechaConsulta}
     * sea verificable sin depender del reloj de la maquina.
     *
     * @param reloj reloj con el que se sella la fecha de consulta
     */
    public GeneradorInformeCrediticio(Clock reloj) {
        this.reloj = Objects.requireNonNull(reloj, "El reloj del generador es obligatorio");
    }

    /**
     * Genera el informe crediticio simulado del documento consultado.
     *
     * La paridad del ultimo digito es el unico discriminador del mock: decide
     * a la vez el rango del score, el estado del titular (D-011: par
     * {@code ACTIVO}, impar {@code EN_MORA}) y si hay reporte negativo.
     *
     * @param numeroDocumento numero de documento, solo digitos
     * @return informe con score, estado, reporte negativo y fecha de consulta
     * @throws IllegalArgumentException si el numero no es una cadena de digitos
     */
    public InformeCrediticioResponse generarInforme(String numeroDocumento) {
        validarNumeroDocumento(numeroDocumento);

        boolean esDocumentoPar = esUltimoDigitoPar(numeroDocumento);
        int score = calcularScore(numeroDocumento, esDocumentoPar);
        EstadoTitular estado = esDocumentoPar ? EstadoTitular.ACTIVO : EstadoTitular.EN_MORA;
        boolean hayReporteNegativo = !esDocumentoPar;

        return new InformeCrediticioResponse(score, estado, hayReporteNegativo, LocalDateTime.now(reloj));
    }

    private static void validarNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isEmpty() || !esSoloDigitos(numeroDocumento)) {
            throw new IllegalArgumentException(
                    "El numero de documento debe ser una cadena de digitos: " + numeroDocumento);
        }
    }

    private static boolean esSoloDigitos(String numeroDocumento) {
        // Recorremos los caracteres como enteros y exigimos que todos sean digitos
        return numeroDocumento.chars().allMatch(Character::isDigit);
    }

    private static boolean esUltimoDigitoPar(String numeroDocumento) {
        int ultimoDigito = Character.digit(numeroDocumento.charAt(numeroDocumento.length() - 1), BASE_DECIMAL);
        return ultimoDigito % DIVISOR_PARIDAD == 0;
    }

    private static int calcularScore(String numeroDocumento, boolean esDocumentoPar) {
        int minimo = esDocumentoPar ? SCORE_MINIMO_SIN_REPORTE : SCORE_MINIMO_CON_REPORTE;
        int maximo = esDocumentoPar ? SCORE_MAXIMO_SIN_REPORTE : SCORE_MAXIMO_CON_REPORTE;
        int amplitud = maximo - minimo + 1;

        // floorMod devuelve siempre un valor en [0, amplitud), incluso con semilla negativa
        return minimo + Math.floorMod(calcularSemilla(numeroDocumento), amplitud);
    }

    private static long calcularSemilla(String numeroDocumento) {
        // Recorremos los caracteres del documento como enteros
        return numeroDocumento.chars()
                // Los promovemos a long para que la mezcla ocurra en 64 bits
                .asLongStream()
                // Partimos del valor base de FNV-1a y mezclamos caracter a caracter:
                // primero el XOR con el caracter, despues la multiplicacion por el primo.
                // El stream es secuencial y ordenado, asi que el resultado es el mismo
                // plegado a izquierda que describe FNV-1a; nunca se paraleliza.
                .reduce(SEMILLA_BASE_FNV, (semilla, caracter) -> (semilla ^ caracter) * FACTOR_FNV);
    }
}
