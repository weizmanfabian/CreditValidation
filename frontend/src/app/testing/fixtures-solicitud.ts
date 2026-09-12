import { ConsultaDeEstado, EstadoDeSolicitud } from '../models/estado-solicitud';
import { SolicitudCreditoRequest, SolicitudCreditoResponse } from '../models/solicitud-credito';

export function crearSolicitudValida(): SolicitudCreditoRequest {
  return {
    tipoDocumento: 'CC',
    numeroDocumento: '1234567890',
    nombres: 'Juan',
    apellidos: 'Pérez',
    correo: 'juan.perez@example.com',
    celular: '3001234567',
    montoSolicitado: 5_000_000,
    plazoMeses: 24,
    ingresosMensuales: 3_500_000,
  };
}

export function crearRespuestaAprobada(): SolicitudCreditoResponse {
  return {
    idSolicitud: 'SOL-20260831-001',
    fechaCreacion: '2026-08-31T10:00:00',
    estado: 'APROBADA',
    solicitante: { nombre: 'Juan Pérez', documento: 'CC 1234567890' },
    detalle: { montoSolicitado: 5_000_000, plazoMeses: 24, tasaEstimada: 1.6 },
    evaluacion: {
      scoreBureau: 720,
      validaciones: [{ nombre: 'Score', resultado: 'APROBADO', detalle: 'Score 720 mayor o igual a 600' }],
    },
    siguientePaso: 'Desembolso en 24 horas',
  };
}

/**
 * Rechazo temprano: el motor corta antes de consultar el buró, así que el
 * `NON_NULL` deja fuera del cuerpo la tasa, el score y el siguiente paso
 * (D-061). Es el caso que el enunciado nombra de forma explícita.
 */
export function crearRespuestaRechazoTemprano(): SolicitudCreditoResponse {
  return {
    idSolicitud: 'SOL-20260831-002',
    fechaCreacion: '2026-08-31T11:30:00',
    estado: 'RECHAZADO_FRAUDE',
    solicitante: { nombre: 'Juan Pérez', documento: 'CC 1010101010' },
    detalle: { montoSolicitado: 5_000_000, plazoMeses: 24 },
    evaluacion: {
      validaciones: [
        { nombre: 'ListaNegra', resultado: 'RECHAZADO', detalle: 'Documento reportado por fraude' },
      ],
    },
  };
}

export function crearRespuestaSinValidaciones(): SolicitudCreditoResponse {
  return { ...crearRespuestaAprobada(), evaluacion: { scoreBureau: 720, validaciones: [] } };
}

export function crearConsultaDeEstado(): ConsultaDeEstado {
  return { tipoDocumento: 'CC', numeroDocumento: '1234567890' };
}

/**
 * Dos solicitudes del mismo documento, de la más reciente a la más antigua
 * (D-054). La segunda trae tasa sellada y la primera no, para que el listado
 * recorra sus dos ramas.
 */
export function crearSolicitudesDelDocumento(): EstadoDeSolicitud[] {
  return [
    {
      idSolicitud: 'SOL-20260831-002',
      fechaCreacion: '2026-08-31T11:30:00',
      estado: 'RECHAZADO',
      montoSolicitado: 8_000_000,
      plazoMeses: 36,
    },
    {
      idSolicitud: 'SOL-20260831-001',
      fechaCreacion: '2026-08-31T10:00:00',
      estado: 'APROBADO',
      montoSolicitado: 5_000_000,
      plazoMeses: 24,
      tasaEstimada: 1.6,
    },
  ];
}
