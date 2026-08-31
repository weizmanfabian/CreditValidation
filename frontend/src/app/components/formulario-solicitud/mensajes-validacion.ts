import { SolicitudCreditoRequest } from '../../models/solicitud-credito';

export type NombreCampoSolicitud = keyof SolicitudCreditoRequest;

export const MENSAJES_VALIDACION: Record<NombreCampoSolicitud, Record<string, string>> = {
  tipoDocumento: {
    required: 'Tipo de documento es requerido y debe ser CC, CE o PA',
  },
  numeroDocumento: {
    required: 'Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos',
    pattern: 'Numero de documento debe tener entre 6 y 15 digitos numericos',
  },
  nombres: {
    required: 'Nombres es requerido',
    maxlength: 'Nombres no debe exceder 100 caracteres',
  },
  apellidos: {
    required: 'Apellidos es requerido',
    maxlength: 'Apellidos no debe exceder 100 caracteres',
  },
  correo: {
    required: 'Correo es requerido',
    email: 'Correo debe tener un formato valido, por ejemplo persona@dominio.com',
    maxlength: 'Correo no debe exceder 255 caracteres',
  },
  celular: {
    required: 'Celular es requerido',
    pattern: 'Celular debe iniciar con 3 y tener 10 digitos',
  },
  montoSolicitado: {
    required: 'Monto solicitado es requerido',
    min: 'Monto solicitado debe ser mayor o igual a 1000000',
    max: 'Monto solicitado no debe exceder 50000000',
  },
  plazoMeses: {
    required: 'Plazo en meses es requerido',
    plazoPermitido: 'Plazo en meses debe ser 12, 24, 36 o 48',
  },
  ingresosMensuales: {
    required: 'Ingresos mensuales es requerido',
    min: 'Ingresos mensuales debe ser mayor que cero',
  },
};
