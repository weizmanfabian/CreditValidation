package com.weiz.motordedecision.api.models.request;

import com.weiz.motordedecision.util.enums.TipoDocumento;

import java.math.BigDecimal;

/**
 * Peticiones de radicacion listas para usar en los tests.
 *
 * Los datos son los del ejemplo del enunciado (lineas 94-124) y son falsos por
 * definicion (docs/conventions.md §11): Juan Perez, documento 1234567890.
 *
 * La peticion valida es la base; cada variante cambia un solo dato, que es lo
 * que hace legible el test que la usa.
 */
public final class PeticionesDePrueba {

    public static final String NUMERO_DOCUMENTO = "1234567890";
    public static final String DOCUMENTO_BLOQUEADO = "1010101010";
    public static final String NOMBRES = "Juan";
    public static final String APELLIDOS = "Pérez";
    public static final String CORREO = "juan.perez@example.com";
    public static final String CELULAR = "3001234567";
    public static final BigDecimal MONTO_SOLICITADO = new BigDecimal("15000000");
    public static final int PLAZO_MESES = 36;
    public static final BigDecimal INGRESOS_MENSUALES = new BigDecimal("4000000");

    private PeticionesDePrueba() {
    }

    /**
     * @return la peticion del ejemplo del enunciado, valida en todos sus campos
     */
    public static SolicitudCreditoRequest crearPeticionValida() {
        return crearPeticionPara(NUMERO_DOCUMENTO, MONTO_SOLICITADO, INGRESOS_MENSUALES);
    }

    /**
     * @param numeroDocumento documento del solicitante
     * @return la peticion valida con ese documento
     */
    public static SolicitudCreditoRequest crearPeticionCon(String numeroDocumento) {
        return crearPeticionPara(numeroDocumento, MONTO_SOLICITADO, INGRESOS_MENSUALES);
    }

    /**
     * @param numeroDocumento documento del solicitante
     * @param montoSolicitado monto pedido
     * @param ingresosMensuales ingresos declarados
     * @return la peticion valida con esas tres cifras
     */
    public static SolicitudCreditoRequest crearPeticionPara(String numeroDocumento,
                                                            BigDecimal montoSolicitado,
                                                            BigDecimal ingresosMensuales) {

        return new SolicitudCreditoRequest(TipoDocumento.CC, numeroDocumento, NOMBRES, APELLIDOS,
                CORREO, CELULAR, montoSolicitado, PLAZO_MESES, ingresosMensuales);
    }
}
