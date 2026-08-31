export const PLAZOS_PERMITIDOS: readonly number[] = [12, 24, 36, 48];

export const REGLAS_SOLICITUD_CREDITO = {
  numeroDocumento: { patron: /^\d{6,15}$/ },
  nombres: { longitudMaxima: 100 },
  apellidos: { longitudMaxima: 100 },
  correo: { longitudMaxima: 255 },
  celular: { patron: /^3\d{9}$/ },
  montoSolicitado: { minimo: 1_000_000, maximo: 50_000_000 },
  ingresosMensuales: { minimo: 1 },
} as const;
