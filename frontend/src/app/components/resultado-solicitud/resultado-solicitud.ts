import { Component, input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';

import { SolicitudCreditoResponse } from '../../models/solicitud-credito';

const CLASE_POR_ESTADO: Record<string, string> = {
  APROBADO: 'estado-aprobado',
  PREAPROBADO: 'estado-preaprobado',
  RECHAZADO: 'estado-rechazado',
  RECHAZADO_FRAUDE: 'estado-rechazado-fraude',
  PENDIENTE_REVISION: 'estado-pendiente-revision',
};

@Component({
  selector: 'app-resultado-solicitud',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './resultado-solicitud.html',
  styleUrl: './resultado-solicitud.css',
})
export class ResultadoSolicitud {

  readonly resultado = input.required<SolicitudCreditoResponse>();

  protected obtenerClaseEstado(): string {
    return CLASE_POR_ESTADO[this.resultado().estado] ?? 'estado-desconocido';
  }
}
