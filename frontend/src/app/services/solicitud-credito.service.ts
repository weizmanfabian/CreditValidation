import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { SolicitudCreditoRequest, SolicitudCreditoResponse } from '../models/solicitud-credito';

export const RUTA_SOLICITUDES = '/api/solicitudes';

@Injectable({ providedIn: 'root' })
export class SolicitudCreditoService {

  private readonly http = inject(HttpClient);

  radicar(solicitud: SolicitudCreditoRequest): Observable<SolicitudCreditoResponse> {
    return this.http.post<SolicitudCreditoResponse>(RUTA_SOLICITUDES, solicitud);
  }
}
