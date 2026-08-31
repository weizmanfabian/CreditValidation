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
