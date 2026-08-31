package com.weiz.motordedecision.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * El paso de una validacion sobre una solicitud, tal como quedo registrado.
 *
 * Replica la tabla {@code resultado_validacion} de
 * {@code db/sql/01-create_schema.sql} (D-025).
 *
 * {@code orden} conserva la posicion en la cadena de validaciones y es
 * informacion de negocio, no un detalle de presentacion: la cadena se detiene
 * en la primera que falla, asi que el numero de filas dice hasta donde llego la
 * evaluacion.
 */
@Entity
@Table(name = "resultado_validacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoValidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    /** {@code SMALLINT} en el esquema: {@code Short}, no {@code Integer}. */
    @Column(name = "orden", nullable = false)
    private Short orden;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;

    @Column(name = "detalle", nullable = false, length = 255)
    private String detalle;
}
