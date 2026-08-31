/**
 * Contrato de error del motor (docs/error-handling.md §2). Los nombres de los
 * campos están en inglés porque son el contrato de cable que publica el backend.
 */
export type UbicacionError = 'body' | 'header' | 'query' | 'path';

export interface ErrorDeCampo {
  field: string;
  message: string;
  location: UbicacionError;
}

export interface RespuestaError {
  message: string;
  codigo?: string;
  detalles?: string;
  errors?: ErrorDeCampo[];
}
