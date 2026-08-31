import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, throwError } from 'rxjs';

import { FormularioSolicitud, convertirASolicitudCredito } from './formulario-solicitud';
import { SolicitudCreditoResponse } from '../../models/solicitud-credito';
import { SolicitudCreditoService } from '../../services/solicitud-credito.service';
import { crearRespuestaAprobada, crearSolicitudValida } from '../../testing/fixtures-solicitud';

describe('FormularioSolicitud', () => {
  let fixture: ComponentFixture<FormularioSolicitud>;
  let servicioSpy: jasmine.SpyObj<SolicitudCreditoService>;

  beforeEach(async () => {
    servicioSpy = jasmine.createSpyObj<SolicitudCreditoService>('SolicitudCreditoService', ['radicar']);
    await TestBed.configureTestingModule({
      imports: [FormularioSolicitud],
      providers: [{ provide: SolicitudCreditoService, useValue: servicioSpy }],
    }).compileComponents();
    fixture = TestBed.createComponent(FormularioSolicitud);
    fixture.detectChanges();
  });

  function obtenerElemento<T extends HTMLElement>(selector: string): T {
    const elemento = (fixture.nativeElement as HTMLElement).querySelector<T>(selector);
    if (elemento === null) {
      throw new Error(`No existe el elemento ${selector}`);
    }
    return elemento;
  }

  function obtenerBotonEnviar(): HTMLButtonElement {
    return obtenerElemento<HTMLButtonElement>('button[type="submit"]');
  }

  function escribirEnCampo(id: string, valor: string): void {
    const entrada = obtenerElemento<HTMLInputElement>(`#${id}`);
    entrada.value = valor;
    entrada.dispatchEvent(new Event('input'));
  }

  function seleccionarOpcionPorTexto(id: string, texto: string): void {
    const selector = obtenerElemento<HTMLSelectElement>(`#${id}`);
    const opcion = Array.from(selector.options).find((candidata) => candidata.text.trim() === texto);
    if (opcion === undefined) {
      throw new Error(`El select #${id} no tiene la opción ${texto}`);
    }
    selector.value = opcion.value;
    selector.dispatchEvent(new Event('change'));
  }

  function llenarFormularioValido(): void {
    const solicitud = crearSolicitudValida();
    seleccionarOpcionPorTexto('tipoDocumento', solicitud.tipoDocumento);
    escribirEnCampo('numeroDocumento', solicitud.numeroDocumento);
    escribirEnCampo('nombres', solicitud.nombres);
    escribirEnCampo('apellidos', solicitud.apellidos);
    escribirEnCampo('correo', solicitud.correo);
    escribirEnCampo('celular', solicitud.celular);
    escribirEnCampo('montoSolicitado', String(solicitud.montoSolicitado));
    seleccionarOpcionPorTexto('plazoMeses', String(solicitud.plazoMeses));
    escribirEnCampo('ingresosMensuales', String(solicitud.ingresosMensuales));
    fixture.detectChanges();
  }

  function enviarFormulario(): void {
    obtenerElemento<HTMLFormElement>('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function textoErroresVisibles(): string {
    const errores = (fixture.nativeElement as HTMLElement).querySelectorAll('.error-campo');
    return Array.from(errores).map((error) => error.textContent?.trim() ?? '').join(' | ');
  }

  describe('formulario inválido', () => {
    it('recién creado, el botón de envío está deshabilitado y no se llama al servicio', () => {
      expect(obtenerBotonEnviar().disabled).toBeTrue();

      enviarFormulario();

      expect(servicioSpy.radicar).not.toHaveBeenCalled();
    });

    it('al tocar y dejar vacío un campo requerido, muestra su mensaje del DTO', () => {
      const entrada = obtenerElemento<HTMLInputElement>('#numeroDocumento');

      entrada.dispatchEvent(new Event('blur'));
      fixture.detectChanges();

      expect(textoErroresVisibles())
          .toContain('Numero de documento es requerido y debe tener entre 6 y 15 digitos numericos');
    });

    const casosInvalidos = [
      { campo: 'numeroDocumento', valor: '12345',
        mensaje: 'Numero de documento debe tener entre 6 y 15 digitos numericos' },
      { campo: 'numeroDocumento', valor: '1234567890123456',
        mensaje: 'Numero de documento debe tener entre 6 y 15 digitos numericos' },
      { campo: 'nombres', valor: 'a'.repeat(101), mensaje: 'Nombres no debe exceder 100 caracteres' },
      { campo: 'apellidos', valor: 'a'.repeat(101), mensaje: 'Apellidos no debe exceder 100 caracteres' },
      { campo: 'correo', valor: 'no-es-un-correo',
        mensaje: 'Correo debe tener un formato valido, por ejemplo persona@dominio.com' },
      { campo: 'celular', valor: '4001234567', mensaje: 'Celular debe iniciar con 3 y tener 10 digitos' },
      { campo: 'celular', valor: '300123456', mensaje: 'Celular debe iniciar con 3 y tener 10 digitos' },
      { campo: 'montoSolicitado', valor: '999999',
        mensaje: 'Monto solicitado debe ser mayor o igual a 1000000' },
      { campo: 'montoSolicitado', valor: '50000001', mensaje: 'Monto solicitado no debe exceder 50000000' },
      { campo: 'ingresosMensuales', valor: '0', mensaje: 'Ingresos mensuales debe ser mayor que cero' },
    ];

    for (const caso of casosInvalidos) {
      it(`con ${caso.campo}=${caso.valor.slice(0, 20)} deshabilita el envío y muestra el mensaje del DTO`, () => {
        llenarFormularioValido();

        escribirEnCampo(caso.campo, caso.valor);
        fixture.detectChanges();

        expect(obtenerBotonEnviar().disabled).withContext('botón deshabilitado').toBeTrue();
        expect(textoErroresVisibles()).toContain(caso.mensaje);
      });
    }
  });

  describe('formulario válido', () => {
    it('con todos los campos válidos, el botón de envío se habilita y no hay mensajes de error', () => {
      llenarFormularioValido();

      expect(obtenerBotonEnviar().disabled).toBeFalse();
      expect(textoErroresVisibles()).toBe('');
    });

    const bordesValidos = [
      { campo: 'numeroDocumento', valor: '123456' },
      { campo: 'numeroDocumento', valor: '123456789012345' },
      { campo: 'montoSolicitado', valor: '1000000' },
      { campo: 'montoSolicitado', valor: '50000000' },
      { campo: 'ingresosMensuales', valor: '1' },
    ];

    for (const caso of bordesValidos) {
      it(`acepta el borde ${caso.campo}=${caso.valor}`, () => {
        llenarFormularioValido();

        escribirEnCampo(caso.campo, caso.valor);
        fixture.detectChanges();

        expect(obtenerBotonEnviar().disabled).toBeFalse();
      });
    }

    it('el select de plazo solo ofrece 12, 24, 36 y 48', () => {
      const opciones = Array.from(obtenerElemento<HTMLSelectElement>('#plazoMeses').options)
          .filter((opcion) => !opcion.disabled)
          .map((opcion) => opcion.text.trim());

      expect(opciones).toEqual(['12', '24', '36', '48']);
    });
  });

  describe('envío', () => {
    it('envía al servicio la solicitud tal como la escribió el usuario', () => {
      servicioSpy.radicar.and.returnValue(new Subject<SolicitudCreditoResponse>());
      llenarFormularioValido();

      enviarFormulario();

      expect(servicioSpy.radicar).toHaveBeenCalledOnceWith(crearSolicitudValida());
    });

    it('mientras el envío está en curso, el botón queda deshabilitado y dice Enviando', () => {
      servicioSpy.radicar.and.returnValue(new Subject<SolicitudCreditoResponse>());
      llenarFormularioValido();

      enviarFormulario();

      const boton = obtenerBotonEnviar();
      expect(boton.disabled).toBeTrue();
      expect(boton.textContent).toContain('Enviando');
    });

    it('cuando el motor responde, el botón vuelve a habilitarse', () => {
      const respuestaPendiente = new Subject<SolicitudCreditoResponse>();
      servicioSpy.radicar.and.returnValue(respuestaPendiente);
      llenarFormularioValido();
      enviarFormulario();

      respuestaPendiente.next(crearRespuestaAprobada());
      respuestaPendiente.complete();
      fixture.detectChanges();

      expect(obtenerBotonEnviar().disabled).toBeFalse();
    });

    it('cuando el motor falla, muestra el error genérico y permite reintentar', () => {
      servicioSpy.radicar.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
      llenarFormularioValido();

      enviarFormulario();

      const errorEnvio = obtenerElemento<HTMLParagraphElement>('.error-envio');
      expect(errorEnvio.textContent).toContain('No fue posible radicar la solicitud');
      expect(obtenerBotonEnviar().disabled).toBeFalse();
    });
  });
});

describe('convertirASolicitudCredito', () => {
  it('con todos los campos presentes, arma la solicitud del API', () => {
    const solicitud = crearSolicitudValida();

    expect(convertirASolicitudCredito({ ...solicitud })).toEqual(solicitud);
  });

  it('con un campo obligatorio en null, devuelve null', () => {
    const valor = { ...crearSolicitudValida(), tipoDocumento: null };

    expect(convertirASolicitudCredito(valor)).toBeNull();
  });
});
