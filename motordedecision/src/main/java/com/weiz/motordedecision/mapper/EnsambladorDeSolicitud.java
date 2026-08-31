package com.weiz.motordedecision.mapper;

import com.weiz.motordedecision.api.models.request.SolicitudCreditoRequest;
import com.weiz.motordedecision.config.RespuestaProperties;
import com.weiz.motordedecision.domain.buro.ResultadoConsultaBuro;
import com.weiz.motordedecision.domain.entities.ResultadoValidacion;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.domain.validacion.RegistroValidacion;
import com.weiz.motordedecision.infraestructura.service.EvaluacionDeSolicitud;
import com.weiz.motordedecision.util.enums.EstadoSolicitud;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Convierte lo que llego por HTTP y lo que decidio el motor en la entidad que
 * se guarda.
 *
 * Es el camino de ida del {@link MapeadorDeSolicitud}, que hace el de vuelta.
 * Aqui viven las tres traducciones que el modelo de datos impone y que ninguna
 * otra capa tiene por que conocer: el estado y el resultado de cada validacion
 * se guardan como texto, el plazo como {@code SMALLINT} y el score solo cuando
 * el buro respondio.
 *
 * El detalle esta en {@link #ensamblarSolicitud}.
 */
@Component
public class EnsambladorDeSolicitud {

    private final Map<EstadoSolicitud, BigDecimal> tasasPorEstado;

    /**
     * @param respuesta tasas por estado; se guarda la que se ofrecio al radicar,
     *                  que es un hecho historico y no cambia si manana cambia la
     *                  configuracion (D-051)
     */
    public EnsambladorDeSolicitud(RespuestaProperties respuesta) {
        this.tasasPorEstado = respuesta.tasaEstimadaPorEstado();
    }

    /**
     * Arma la solicitud lista para guardar, con su rastro de validaciones
     * enlazado.
     *
     * Ni el identificador de negocio ni la fecha de creacion se ponen aqui: los
     * resuelve {@code SolicitudDataAccessor} al guardar, porque el consecutivo
     * del identificador depende del dia que quede en esa misma fecha (D-025).
     *
     * @param peticion datos del formulario, ya validados de formato
     * @param evaluacion estado decidido, informe del buro y rastro de la cadena
     * @return la solicitud con sus filas hijas, sin persistir
     */
    public Solicitud ensamblarSolicitud(SolicitudCreditoRequest peticion, EvaluacionDeSolicitud evaluacion) {
        Solicitud solicitud = Solicitud.builder()
                .tipoDocumento(peticion.tipoDocumento().name())
                .numeroDocumento(peticion.numeroDocumento())
                .nombres(peticion.nombres())
                .apellidos(peticion.apellidos())
                .correoElectronico(peticion.correo())
                .telefonoCelular(peticion.celular())
                .montoSolicitado(peticion.montoSolicitado())
                .plazoMeses(peticion.plazoMeses().shortValue())
                .ingresosMensuales(peticion.ingresosMensuales())
                .estado(evaluacion.estado().name())
                .scoreBuro(resolverScore(evaluacion.informeBuro()))
                .tasaEstimada(tasasPorEstado.get(evaluacion.estado()))
                .build();

        // Cada registro de la cadena se guarda como una fila hija, en el mismo
        // orden en que se ejecuto
        evaluacion.validaciones().stream()
                .map(EnsambladorDeSolicitud::convertirAFilaDeValidacion)
                .forEach(solicitud::agregarResultado);

        return solicitud;
    }

    /**
     * El score solo se guarda cuando hubo informe. Sin informe la columna queda
     * nula, que es distinto de un cero: cero seria un score malisimo, y no lo
     * hay.
     */
    private static Integer resolverScore(ResultadoConsultaBuro informeBuro) {
        return informeBuro.hayInformeDisponible() ? informeBuro.score() : null;
    }

    private static ResultadoValidacion convertirAFilaDeValidacion(RegistroValidacion registro) {
        return ResultadoValidacion.builder()
                .orden(registro.orden())
                .nombre(registro.nombre())
                .resultado(registro.resultado().name())
                .detalle(registro.detalle())
                .build();
    }
}
