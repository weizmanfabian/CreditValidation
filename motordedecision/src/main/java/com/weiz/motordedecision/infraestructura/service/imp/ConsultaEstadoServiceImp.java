package com.weiz.motordedecision.infraestructura.service.imp;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor;
import com.weiz.motordedecision.infraestructura.service.ConsultaEstadoService;
import com.weiz.motordedecision.mapper.MapeadorDeConsultaDeEstado;
import com.weiz.motordedecision.util.enums.TipoDocumento;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquesta la consulta de estado: busca por documento y mapea cada solicitud
 * al elemento de la lista.
 *
 * No reimplementa nada: la busqueda —con su orden de la mas reciente a la mas
 * antigua y su envoltura de errores de la base— ya la resuelve
 * {@link SolicitudDataAccessor#buscarPorDocumento} desde la feature 8, y la
 * conversion al modelo de respuesta es de {@link MapeadorDeConsultaDeEstado}.
 */
@Service
public class ConsultaEstadoServiceImp implements ConsultaEstadoService {

    private final SolicitudDataAccessor solicitudDataAccessor;
    private final MapeadorDeConsultaDeEstado mapeadorDeConsultaDeEstado;

    public ConsultaEstadoServiceImp(SolicitudDataAccessor solicitudDataAccessor,
                                    MapeadorDeConsultaDeEstado mapeadorDeConsultaDeEstado) {

        this.solicitudDataAccessor = solicitudDataAccessor;
        this.mapeadorDeConsultaDeEstado = mapeadorDeConsultaDeEstado;
    }

    /**
     * El tipo llega como enum y la base lo guarda como texto: la traduccion
     * con {@code name()} vive aqui, en la unica capa que conoce a los dos.
     */
    @Override
    public List<EstadoDeSolicitudResponse> consultarPorDocumento(TipoDocumento tipoDocumento,
                                                                 String numeroDocumento) {

        return solicitudDataAccessor.buscarPorDocumento(tipoDocumento.name(), numeroDocumento).stream()
                // Cada solicitud persistida se convierte en un elemento de la lista
                .map(mapeadorDeConsultaDeEstado::mapearAEstado)
                // El orden ya viene resuelto de la consulta: de la mas reciente a la mas antigua
                .toList();
    }
}
