import { Component, input } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';

import { obtenerClaseDeEstado } from '../../models/estilos-estado';
import { SolicitudCreditoResponse } from '../../models/solicitud-credito';

@Component({
  selector: 'app-resultado-solicitud',
  imports: [DatePipe, DecimalPipe],
  templateUrl: './resultado-solicitud.html',
  styleUrl: './resultado-solicitud.css',
})
export class ResultadoSolicitud {

  readonly resultado = input.required<SolicitudCreditoResponse>();

  protected obtenerClaseEstado(): string {
    return obtenerClaseDeEstado(this.resultado().estado);
  }
}
