package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.domain.entities.ResultadoValidacion;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import com.weiz.motordedecision.util.enums.ResultadoEvaluacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Solicitudes ya evaluadas y persistidas, tal como le llegan al mapeador.
 *
 * Los datos son los del ejemplo del enunciado (lineas 94-124), que ademas son
 * falsos por definicion (docs/conventions.md §11): Juan Perez, documento
 * 1234567890.
 *
 * Los detalles de las validaciones son los que escribe la cadena de verdad, no
 * los que invente el test. El unico que se aparta del ejemplo del enunciado es
 * el de Score: la cadena corta en 600, que es su umbral real, asi que rinde
 * {@code "Score 750 >= 600"} y no {@code ">= 700"} (decidido y commiteado en
 * 4767e0a).
 */
public final class SolicitudesPersistidasDePrueba {

    public static final String ID_SOLICITUD = "SOL-20260824-001";
    public static final LocalDateTime FECHA_CREACION = LocalDateTime.of(2026, 8, 24, 10, 30);
    public static final String NOMBRE_COMPLETO = "Juan Pérez";
    public static final String DOCUMENTO_CON_TIPO = "CC 1234567890";
    public static final BigDecimal MONTO_SOLICITADO = new BigDecimal("15000000");
    public static final short PLAZO_MESES = 36;
    public static final int SCORE = 750;
    public static final String DETALLE_DE_SCORE = "Score 750 >= 600";
    public static final String DETALLE_DE_IDENTIDAD_BLOQUEADA = "Documento reportado en la lista de bloqueados";

    private static final String TIPO_DOCUMENTO = "CC";
    private static final String NUMERO_DOCUMENTO = "1234567890";
    private static final String NOMBRES = "Juan";
    private static final String APELLIDOS = "Pérez";
    private static final String CORREO = "juan.perez@example.com";
    private static final String CELULAR = "3001234567";
    private static final BigDecimal INGRESOS = new BigDecimal("4000000");

    private SolicitudesPersistidasDePrueba() {
    }

    /**
     * Crea la solicitud del ejemplo del enunciado: la cadena completa aprobada
     * y el buro con score.
     *
     * @param estado estado final que decidio el motor
     * @return solicitud con sus cuatro validaciones aprobadas
     */
    public static Solicitud crearSolicitudConLaCadenaCompleta(EstadoSolicitud estado) {
        Solicitud solicitud = crearSolicitudBase(estado, SCORE);
        agregarResultado(solicitud, 1, "Identidad", ResultadoEvaluacion.APROBADO, "Documento no bloqueado");
        agregarResultado(solicitud, 2, "Score", ResultadoEvaluacion.APROBADO, DETALLE_DE_SCORE);
        agregarResultado(solicitud, 3, "Capacidad de pago", ResultadoEvaluacion.APROBADO,
                "Monto dentro del rango permitido");
        agregarResultado(solicitud, 4, "Reporte negativo", ResultadoEvaluacion.APROBADO, "Sin reportes negativos");
        return solicitud;
    }

    /**
     * Crea la solicitud de un documento bloqueado: la cadena corto en la
     * primera validacion y nunca se consulto el buro, asi que no hay score.
     *
     * @return solicitud en estado RECHAZADO_FRAUDE
     */
    public static Solicitud crearSolicitudRechazadaPorDocumentoBloqueado() {
        Solicitud solicitud = crearSolicitudBase(EstadoSolicitud.RECHAZADO_FRAUDE, null);
        agregarResultado(solicitud, 1, "Identidad", ResultadoEvaluacion.RECHAZADO,
                DETALLE_DE_IDENTIDAD_BLOQUEADA);
        return solicitud;
    }

    private static Solicitud crearSolicitudBase(EstadoSolicitud estado, Integer score) {
        return Solicitud.builder()
                .idSolicitud(ID_SOLICITUD)
                .tipoDocumento(TIPO_DOCUMENTO)
                .numeroDocumento(NUMERO_DOCUMENTO)
                .nombres(NOMBRES)
                .apellidos(APELLIDOS)
                .correoElectronico(CORREO)
                .telefonoCelular(CELULAR)
                .montoSolicitado(MONTO_SOLICITADO)
                .plazoMeses(PLAZO_MESES)
                .ingresosMensuales(INGRESOS)
                .estado(estado.name())
                .scoreBuro(score)
                .fechaCreacion(FECHA_CREACION)
                .resultados(new ArrayList<>())
                .build();
    }

    private static void agregarResultado(Solicitud solicitud,
                                         int orden,
                                         String nombre,
                                         ResultadoEvaluacion resultado,
                                         String detalle) {

        solicitud.agregarResultado(ResultadoValidacion.builder()
                .orden((short) orden)
                .nombre(nombre)
                .resultado(resultado.name())
                .detalle(detalle)
                .build());
    }
}
