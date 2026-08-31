import { TipoDocumento } from './solicitud-credito';

/**
 * Un elemento de la lista que responde
 * `GET /api/solicitudes/{tipoDocumento}/{numeroDocumento}`, espejo de
 * `EstadoDeSolicitudResponse` del motor.
 *
 * El motor serializa con NON_NULL: la tasa no viaja cuando no hubo oferta, así
 * que aquí es opcional, no nula.
 */
export interface EstadoDeSolicitud {
  idSolicitud: string;
  fechaCreacion: string;
  estado: string;
  montoSolicitado: number;
  plazoMeses: number;
  tasaEstimada?: number | null;
}

export interface ConsultaDeEstado {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
}
