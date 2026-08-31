const CLASE_POR_ESTADO: Record<string, string> = {
  APROBADO: 'estado-aprobado',
  PREAPROBADO: 'estado-preaprobado',
  RECHAZADO: 'estado-rechazado',
  RECHAZADO_FRAUDE: 'estado-rechazado-fraude',
  PENDIENTE_REVISION: 'estado-pendiente-revision',
};

/**
 * Traduce un estado del catálogo a la clase CSS de su distintivo. Las clases
 * viven en `styles.css` porque las comparten el resultado de la radicación y el
 * listado de la consulta de estado.
 */
export function obtenerClaseDeEstado(estado: string): string {
  return CLASE_POR_ESTADO[estado] ?? 'estado-desconocido';
}
