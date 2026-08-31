import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { ConsultaDeEstado, EstadoDeSolicitud } from '../models/estado-solicitud';
import { RUTA_SOLICITUDES } from './solicitud-credito.service';

@Injectable({ providedIn: 'root' })
export class ConsultaEstadoService {

  private readonly http = inject(HttpClient);

  consultarPorDocumento(consulta: ConsultaDeEstado): Observable<EstadoDeSolicitud[]> {
    const numero = encodeURIComponent(consulta.numeroDocumento);
    return this.http.get<EstadoDeSolicitud[]>(`${RUTA_SOLICITUDES}/${consulta.tipoDocumento}/${numero}`);
  }
}
