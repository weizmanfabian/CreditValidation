import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { obtenerMensajeDeError } from '../../forms/mensaje-de-error';
import { MENSAJES_VALIDACION } from '../../forms/mensajes-validacion';
import { ConsultaDeEstado, EstadoDeSolicitud } from '../../models/estado-solicitud';
import { obtenerClaseDeEstado } from '../../models/estilos-estado';
import { REGLAS_SOLICITUD_CREDITO } from '../../models/reglas-solicitud-credito';
import { TipoDocumento } from '../../models/solicitud-credito';
import { ConsultaEstadoService } from '../../services/consulta-estado.service';

export type NombreCampoConsulta = 'tipoDocumento' | 'numeroDocumento';

export interface ValorFormularioConsulta {
  tipoDocumento: TipoDocumento | null;
  numeroDocumento: string;
}

export function convertirAConsultaDeEstado(valor: ValorFormularioConsulta): ConsultaDeEstado | null {
  const { tipoDocumento, numeroDocumento } = valor;
  return tipoDocumento === null ? null : { tipoDocumento, numeroDocumento };
}

@Component({
  selector: 'app-consulta-estado',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './consulta-estado.html',
  styleUrl: './consulta-estado.css',
})
export class ConsultaEstado {

  protected readonly tiposDocumento: readonly TipoDocumento[] = ['CC', 'CE', 'PA'];

  protected readonly consultando = signal(false);
  protected readonly errorConsulta = signal(false);
  protected readonly solicitudes = signal<EstadoDeSolicitud[] | null>(null);

  protected readonly formulario = new FormGroup({
    tipoDocumento: new FormControl<TipoDocumento | null>(null, Validators.required),
    numeroDocumento: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(REGLAS_SOLICITUD_CREDITO.numeroDocumento.patron)],
    }),
  });

  private readonly consultaEstadoService = inject(ConsultaEstadoService);
  private readonly destroyRef = inject(DestroyRef);

  protected consultar(): void {
    if (this.formulario.invalid || this.consultando()) {
      this.formulario.markAllAsTouched();
      return;
    }
    const consulta = convertirAConsultaDeEstado(this.formulario.getRawValue());
    if (consulta === null) {
      return;
    }
    this.consultarPorDocumento(consulta);
  }

  protected obtenerErrorVisible(nombreCampo: NombreCampoConsulta): string | null {
    return obtenerMensajeDeError(this.formulario.controls[nombreCampo], MENSAJES_VALIDACION[nombreCampo]);
  }

  protected obtenerClaseEstado(estado: string): string {
    return obtenerClaseDeEstado(estado);
  }

  private consultarPorDocumento(consulta: ConsultaDeEstado): void {
    this.consultando.set(true);
    this.errorConsulta.set(false);
    this.solicitudes.set(null);
    this.consultaEstadoService.consultarPorDocumento(consulta)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (solicitudes) => {
          this.solicitudes.set(solicitudes);
          this.consultando.set(false);
        },
        error: () => {
          this.errorConsulta.set(true);
          this.consultando.set(false);
        },
      });
  }
}
