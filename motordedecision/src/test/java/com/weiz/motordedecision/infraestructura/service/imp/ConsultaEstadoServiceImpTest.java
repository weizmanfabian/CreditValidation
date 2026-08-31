package com.weiz.motordedecision.infraestructura.service.imp;

import com.weiz.motordedecision.api.models.response.EstadoDeSolicitudResponse;
import com.weiz.motordedecision.domain.dataaccessors.SolicitudDataAccessor;
import com.weiz.motordedecision.domain.entities.Solicitud;
import com.weiz.motordedecision.mapper.MapeadorDeConsultaDeEstado;
import com.weiz.motordedecision.util.enums.TipoDocumento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * La orquestacion de la consulta de estado con dobles: traduce el tipo de
 * documento al texto que guarda la base, delega la busqueda en el accessor
 * —que ya trae el orden resuelto— y mapea cada solicitud conservando ese
 * orden.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio de consulta de estado")
class ConsultaEstadoServiceImpTest {

    private static final String NUMERO_DOCUMENTO = "1234567890";

    @Mock
    private SolicitudDataAccessor solicitudDataAccessor;

    @Mock
    private MapeadorDeConsultaDeEstado mapeadorDeConsultaDeEstado;

    @InjectMocks
    private ConsultaEstadoServiceImp servicio;

    @Test
    @DisplayName("Con solicitudes: busca con el tipo como texto y mapea en el orden del accessor")
    void consultarPorDocumento_conSolicitudesPersistidas_buscaConElTipoComoTextoYMapeaEnOrden() {
        Solicitud masReciente = new Solicitud();
        Solicitud masAntigua = new Solicitud();
        EstadoDeSolicitudResponse elementoReciente = crearElemento("SOL-20260831-001");
        EstadoDeSolicitudResponse elementoAntiguo = crearElemento("SOL-20260830-001");
        when(solicitudDataAccessor.buscarPorDocumento(TipoDocumento.CC.name(), NUMERO_DOCUMENTO))
                .thenReturn(List.of(masReciente, masAntigua));
        when(mapeadorDeConsultaDeEstado.mapearAEstado(masReciente)).thenReturn(elementoReciente);
        when(mapeadorDeConsultaDeEstado.mapearAEstado(masAntigua)).thenReturn(elementoAntiguo);

        List<EstadoDeSolicitudResponse> elementos =
                servicio.consultarPorDocumento(TipoDocumento.CC, NUMERO_DOCUMENTO);

        assertThat(elementos).containsExactly(elementoReciente, elementoAntiguo);
        verify(solicitudDataAccessor).buscarPorDocumento("CC", NUMERO_DOCUMENTO);
    }

    @Test
    @DisplayName("Sin solicitudes: devuelve la lista vacia y no mapea nada")
    void consultarPorDocumento_sinSolicitudes_devuelveListaVaciaSinMapearNada() {
        when(solicitudDataAccessor.buscarPorDocumento(TipoDocumento.CE.name(), NUMERO_DOCUMENTO))
                .thenReturn(List.of());

        List<EstadoDeSolicitudResponse> elementos =
                servicio.consultarPorDocumento(TipoDocumento.CE, NUMERO_DOCUMENTO);

        assertThat(elementos).isEmpty();
        verifyNoInteractions(mapeadorDeConsultaDeEstado);
    }

    private static EstadoDeSolicitudResponse crearElemento(String idSolicitud) {
        return new EstadoDeSolicitudResponse(idSolicitud, LocalDateTime.of(2026, 8, 31, 10, 30),
                "APROBADO", new BigDecimal("15000000"), (short) 36, new BigDecimal("1.20"));
    }
}
