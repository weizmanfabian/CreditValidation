package com.weiz.buro.util.exceptions.tecnica;

import com.weiz.buro.util.exceptions.ExcepcionTecnica;

/**
 * Doble de prueba de la rama tecnica del manejador.
 *
 * El buro es un simulador sin dependencias externas, asi que hoy no tiene
 * ninguna excepcion tecnica propia (D-013 de {@code docs/decisions.md}): esta
 * vive en {@code src/test} para ejercitar el contrato —500 generico, sin filtrar
 * el mensaje interno y con log ERROR— sin dejar codigo muerto en produccion.
 */
public class ExcepcionTecnicaDePrueba extends ExcepcionTecnica {

    public ExcepcionTecnicaDePrueba(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
