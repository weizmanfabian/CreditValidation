import { Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { RespuestaError } from '../../models/error-respuesta';
import { SolicitudCreditoRequest, SolicitudCreditoResponse, TipoDocumento } from '../../models/solicitud-credito';
import { PLAZOS_PERMITIDOS, REGLAS_SOLICITUD_CREDITO } from '../../models/reglas-solicitud-credito';
import { SolicitudCreditoService } from '../../services/solicitud-credito.service';
import { validarPlazoPermitido } from '../../validators/plazo-permitido';
import { ResultadoSolicitud } from '../resultado-solicitud/resultado-solicitud';
import { MENSAJES_VALIDACION, NombreCampoSolicitud } from './mensajes-validacion';

export interface ValorFormularioSolicitud {
  tipoDocumento: TipoDocumento | null;
  numeroDocumento: string;
  nombres: string;
  apellidos: string;
  correo: string;
  celular: string;
  montoSolicitado: number | null;
  plazoMeses: number | null;
  ingresosMensuales: number | null;
}

export interface ErroresValidacionDelMotor {
  mensaje: string;
  porCampo: Partial<Record<NombreCampoSolicitud, string>>;
  generales: string[];
}

export function convertirASolicitudCredito(valor: ValorFormularioSolicitud): SolicitudCreditoRequest | null {
  const { tipoDocumento, montoSolicitado, plazoMeses, ingresosMensuales } = valor;
  if (tipoDocumento === null || montoSolicitado === null
      || plazoMeses === null || ingresosMensuales === null) {
    return null;
  }
  return { ...valor, tipoDocumento, montoSolicitado, plazoMeses, ingresosMensuales };
}

export function extraerErrorDeValidacion(error: unknown): RespuestaError | null {
  if (!(error instanceof HttpErrorResponse) || error.status !== 400) {
    return null;
  }
  const cuerpo: unknown = error.error;
  if (typeof cuerpo !== 'object' || cuerpo === null || !('errors' in cuerpo)) {
    return null;
  }
  const candidato = cuerpo as RespuestaError;
  return Array.isArray(candidato.errors) ? candidato : null;
}

export function agruparErroresDeValidacion(respuesta: RespuestaError): ErroresValidacionDelMotor {
  const porCampo: Partial<Record<NombreCampoSolicitud, string>> = {};
  const generales: string[] = [];
  for (const errorDeCampo of respuesta.errors ?? []) {
    const campo = errorDeCampo.field;
    if (esCampoDelFormulario(campo)) {
      porCampo[campo] ??= errorDeCampo.message;
    } else {
      generales.push(errorDeCampo.message);
    }
  }
  return { mensaje: respuesta.message, porCampo, generales };
}

function esCampoDelFormulario(campo: string): campo is NombreCampoSolicitud {
  return campo in MENSAJES_VALIDACION;
}

@Component({
  selector: 'app-formulario-solicitud',
  imports: [ReactiveFormsModule, ResultadoSolicitud],
  templateUrl: './formulario-solicitud.html',
  styleUrl: './formulario-solicitud.css',
})
export class FormularioSolicitud {

  protected readonly tiposDocumento: readonly TipoDocumento[] = ['CC', 'CE', 'PA'];
  protected readonly plazosPermitidos = PLAZOS_PERMITIDOS;

  protected readonly enviando = signal(false);
  protected readonly errorEnvio = signal(false);
  protected readonly erroresDelMotor = signal<ErroresValidacionDelMotor | null>(null);
  protected readonly resultado = signal<SolicitudCreditoResponse | null>(null);

  protected readonly formulario = new FormGroup({
    tipoDocumento: new FormControl<TipoDocumento | null>(null, Validators.required),
    numeroDocumento: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(REGLAS_SOLICITUD_CREDITO.numeroDocumento.patron)],
    }),
    nombres: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(REGLAS_SOLICITUD_CREDITO.nombres.longitudMaxima)],
    }),
    apellidos: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(REGLAS_SOLICITUD_CREDITO.apellidos.longitudMaxima)],
    }),
    correo: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.email,
        Validators.maxLength(REGLAS_SOLICITUD_CREDITO.correo.longitudMaxima),
      ],
    }),
    celular: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(REGLAS_SOLICITUD_CREDITO.celular.patron)],
    }),
    montoSolicitado: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(REGLAS_SOLICITUD_CREDITO.montoSolicitado.minimo),
      Validators.max(REGLAS_SOLICITUD_CREDITO.montoSolicitado.maximo),
    ]),
    plazoMeses: new FormControl<number | null>(null, [Validators.required, validarPlazoPermitido]),
    ingresosMensuales: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(REGLAS_SOLICITUD_CREDITO.ingresosMensuales.minimo),
    ]),
  });

  private readonly solicitudCreditoService = inject(SolicitudCreditoService);
  private readonly destroyRef = inject(DestroyRef);

  protected enviar(): void {
    if (this.formulario.invalid || this.enviando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const solicitud = convertirASolicitudCredito(this.formulario.getRawValue());
    if (solicitud === null) {
      return;
    }
    this.radicarSolicitud(solicitud);
  }

  protected obtenerErrorVisible(nombreCampo: NombreCampoSolicitud): string | null {
    return this.obtenerErrorLocal(nombreCampo) ?? this.erroresDelMotor()?.porCampo[nombreCampo] ?? null;
  }

  private obtenerErrorLocal(nombreCampo: NombreCampoSolicitud): string | null {
    const control = this.formulario.controls[nombreCampo];
    if (control.valid || !(control.touched || control.dirty)) {
      return null;
    }
    const mensajesDelCampo = MENSAJES_VALIDACION[nombreCampo];
    const claveError = Object.keys(control.errors ?? {}).find((clave) => clave in mensajesDelCampo);
    return claveError === undefined ? null : mensajesDelCampo[claveError];
  }

  private radicarSolicitud(solicitud: SolicitudCreditoRequest): void {
    this.enviando.set(true);
    this.errorEnvio.set(false);
    this.erroresDelMotor.set(null);
    this.resultado.set(null);
    this.solicitudCreditoService.radicar(solicitud)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (respuesta) => {
          this.resultado.set(respuesta);
          this.enviando.set(false);
        },
        error: (error: unknown) => {
          this.manejarErrorDeEnvio(error);
          this.enviando.set(false);
        },
      });
  }

  private manejarErrorDeEnvio(error: unknown): void {
    const errorDeValidacion = extraerErrorDeValidacion(error);
    if (errorDeValidacion === null) {
      this.errorEnvio.set(true);
      return;
    }
    this.erroresDelMotor.set(agruparErroresDeValidacion(errorDeValidacion));
  }
}
