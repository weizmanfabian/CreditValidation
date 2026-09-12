import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject, throwError } from 'rxjs';

import { ConsultaEstado, convertirAConsultaDeEstado } from './consulta-estado';
import { EstadoDeSolicitud } from '../../models/estado-solicitud';
import { ConsultaEstadoService } from '../../services/consulta-estado.service';
import { crearConsultaDeEstado, crearSolicitudesDelDocumento } from '../../testing/fixtures-solicitud';

describe('ConsultaEstado', () => {
  let fixture: ComponentFixture<ConsultaEstado>;
  let servicioSpy: jasmine.SpyObj<ConsultaEstadoService>;

  beforeEach(async () => {
    servicioSpy = jasmine.createSpyObj<ConsultaEstadoService>('ConsultaEstadoService', ['consultarPorDocumento']);
    await TestBed.configureTestingModule({
      imports: [ConsultaEstado],
      providers: [{ provide: ConsultaEstadoService, useValue: servicioSpy }],
    }).compileComponents();
    fixture = TestBed.createComponent(ConsultaEstado);
    fixture.detectChanges();
  });

  function obtenerElemento<T extends HTMLElement>(selector: string): T {
    const elemento = (fixture.nativeElement as HTMLElement).querySelector<T>(selector);
    if (elemento === null) {
      throw new Error(`No existe el elemento ${selector}`);
    }
    return elemento;
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

  function llenarConsultaValida(): void {
    const consulta = crearConsultaDeEstado();
    seleccionarOpcionPorTexto('tipoDocumentoConsulta', consulta.tipoDocumento);
    escribirEnCampo('numeroDocumentoConsulta', consulta.numeroDocumento);
    fixture.detectChanges();
  }

  function enviarConsulta(): void {
    obtenerElemento<HTMLFormElement>('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function responderCon(solicitudes: EstadoDeSolicitud[]): void {
    const respuestaPendiente = new Subject<EstadoDeSolicitud[]>();
    servicioSpy.consultarPorDocumento.and.returnValue(respuestaPendiente);
    llenarConsultaValida();
    enviarConsulta();

    respuestaPendiente.next(solicitudes);
    respuestaPendiente.complete();
    fixture.detectChanges();
  }

  it('con el formulario vacío, el botón está deshabilitado y no se llama al servicio', () => {
    expect(obtenerElemento<HTMLButtonElement>('button[type="submit"]').disabled).toBeTrue();

    enviarConsulta();

    expect(servicioSpy.consultarPorDocumento).not.toHaveBeenCalled();
  });

  it('con dos solicitudes, pinta el listado con su estado y su fecha', () => {
    responderCon(crearSolicitudesDelDocumento());

    const html = fixture.nativeElement as HTMLElement;
    expect(servicioSpy.consultarPorDocumento).toHaveBeenCalledOnceWith(crearConsultaDeEstado());
    expect(html.querySelectorAll('.solicitud').length).toBe(2);
    expect(html.textContent).withContext('identificador de la primera').toContain('SOL-20260831-002');
    expect(html.textContent).withContext('identificador de la segunda').toContain('SOL-20260831-001');
    expect(html.querySelector('.sin-solicitudes')).toBeNull();
  });

  it('con un documento sin solicitudes, pinta el mensaje de lista vacía', () => {
    responderCon([]);

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.sin-solicitudes')?.textContent)
        .toContain('Este documento no tiene solicitudes registradas');
    expect(html.querySelectorAll('.solicitud').length).toBe(0);
  });

  it('cuando la consulta falla, pinta .error-consulta y permite reintentar', () => {
    servicioSpy.consultarPorDocumento.and.returnValue(throwError(() => new Error('motor caído')));
    llenarConsultaValida();

    enviarConsulta();

    expect(obtenerElemento<HTMLParagraphElement>('.error-consulta').textContent)
        .toContain('No fue posible consultar las solicitudes');
    expect(obtenerElemento<HTMLButtonElement>('button[type="submit"]').disabled).toBeFalse();
  });
});

describe('convertirAConsultaDeEstado', () => {
  it('con el tipo de documento presente, arma la consulta del API', () => {
    const consulta = crearConsultaDeEstado();

    expect(convertirAConsultaDeEstado({ ...consulta })).toEqual(consulta);
  });

  it('con el tipo de documento en null, devuelve null', () => {
    const valor = { ...crearConsultaDeEstado(), tipoDocumento: null };

    expect(convertirAConsultaDeEstado(valor)).toBeNull();
  });
});
