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

export interface SeccionDetalleFinanciero {
  montoSolicitado: number;
  plazoMeses: number;
  tasaEstimada: number | null;
}

export interface ValidacionRealizada {
  nombre: string;
  resultado: string;
  detalle: string;
}

export interface SeccionEvaluacion {
  scoreBureau: number | null;
  validaciones: ValidacionRealizada[];
}

export interface SolicitudCreditoResponse {
  idSolicitud: string;
  fechaCreacion: string;
  estado: string;
  solicitante: SeccionSolicitante;
  detalle: SeccionDetalleFinanciero;
  evaluacion: SeccionEvaluacion;
  siguientePaso: string | null;
}
