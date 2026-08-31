package com.weiz.motordedecision.domain.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una solicitud de credito radicada y evaluada.
 *
 * El mapeo replica columna a columna la tabla {@code solicitud} de
 * {@code db/sql/01-create_schema.sql}, que es la fuente de verdad del modelo de
 * datos (D-025): ninguna columna se anade aqui sin actualizar antes el script.
 *
 * Lleva dos identificadores a proposito. {@code id} es la clave tecnica, la que
 * referencian las filas de {@link ResultadoValidacion}. {@code idSolicitud} es
 * el identificador de negocio {@code SOL-yyyyMMdd-NNN} que viaja al cliente; su
 * consecutivo lo calcula la aplicacion, no una secuencia de la base.
 *
 * {@code scoreBuro} y {@code tasaEstimada} son nulos a proposito: no todas las
 * solicitudes llegan a consultar el buro ni a calcular una tasa.
 */
@Entity
@Table(name = "solicitud")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "id_solicitud", nullable = false, unique = true, length = 20)
    private String idSolicitud;

    @Column(name = "tipo_documento", nullable = false, length = 2)
    private String tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo_electronico", nullable = false, length = 255)
    private String correoElectronico;

    @Column(name = "telefono_celular", nullable = false, length = 20)
    private String telefonoCelular;

    @Column(name = "monto_solicitado", nullable = false, precision = 15, scale = 2)
    private BigDecimal montoSolicitado;

    /** {@code SMALLINT} en el esquema: {@code Short}, no {@code Integer}. */
    @Column(name = "plazo_meses", nullable = false)
    private Short plazoMeses;

    @Column(name = "ingresos_mensuales", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresosMensuales;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "score_buro")
    private Integer scoreBuro;

    @Column(name = "tasa_estimada", precision = 5, scale = 2)
    private BigDecimal tasaEstimada;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Rastro de la cadena de validaciones, en el orden en que se ejecutaron.
     *
     * La cascada y el {@code orphanRemoval} replican el
     * {@code ON DELETE CASCADE} de la llave foranea: las filas hijas no tienen
     * vida propia fuera de su solicitud.
     */
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    @Builder.Default
    private List<ResultadoValidacion> resultados = new ArrayList<>();

    /**
     * Anade un resultado a la cadena dejando las dos puntas de la relacion
     * enlazadas, que es lo que hace que la cascada persista la fila hija.
     *
     * @param resultado resultado de una validacion, con su posicion en la cadena
     */
    public void agregarResultado(ResultadoValidacion resultado) {
        resultados.add(resultado);
        resultado.setSolicitud(this);
    }
}
