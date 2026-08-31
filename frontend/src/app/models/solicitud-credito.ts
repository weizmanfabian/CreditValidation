export type TipoDocumento = 'CC' | 'CE' | 'PA';

export interface SolicitudCreditoRequest {
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  correo: string;
  celular: string;
  montoSolicitado: number;
  plazoMeses: number;
  ingresosMensuales: number;
}

export interface SeccionSolicitante {
  nombre: string;
  documento: string;
}

/**
 * El motor serializa con NON_NULL: la tasa, el score y el siguiente paso no
 * viajan cuando no aplican, asi que aqui son opcionales, no nulos.
 */
export interface SeccionDetalleFinanciero {
  montoSolicitado: number;
  plazoMeses: number;
  tasaEstimada?: number | null;
}

export interface ValidacionRealizada {
  nombre: string;
  resultado: string;
  detalle: string;
}

export interface SeccionEvaluacion {
  scoreBureau?: number | null;
  validaciones: ValidacionRealizada[];
}

export interface SolicitudCreditoResponse {
  idSolicitud: string;
  fechaCreacion: string;
  estado: string;
  solicitante: SeccionSolicitante;
  detalle: SeccionDetalleFinanciero;
  evaluacion: SeccionEvaluacion;
  siguientePaso?: string | null;
}
