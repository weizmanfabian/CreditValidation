import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResultadoSolicitud } from './resultado-solicitud';
import { SolicitudCreditoResponse } from '../../models/solicitud-credito';
import {
  crearRespuestaAprobada,
  crearRespuestaRechazoTemprano,
  crearRespuestaSinValidaciones,
} from '../../testing/fixtures-solicitud';

describe('ResultadoSolicitud', () => {
  let fixture: ComponentFixture<ResultadoSolicitud>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ResultadoSolicitud] }).compileComponents();
    fixture = TestBed.createComponent(ResultadoSolicitud);
  });

  function pintarResultado(resultado: SolicitudCreditoResponse): HTMLElement {
    fixture.componentRef.setInput('resultado', resultado);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function obtenerTextoVisible(html: HTMLElement): string {
    return html.textContent ?? '';
  }

  it('con una respuesta aprobada completa, pinta tasa, score, validaciones y siguiente paso', () => {
    const aprobada = crearRespuestaAprobada();

    const html = pintarResultado(aprobada);

    const texto = obtenerTextoVisible(html);
    expect(texto).withContext('radicado').toContain(aprobada.idSolicitud);
    expect(texto).withContext('tasa estimada').toContain('Tasa estimada');
    expect(texto).withContext('score del buró').toContain('Score del buró');
    expect(html.querySelector('.siguiente-paso')?.textContent).toContain('Desembolso en 24 horas');
    expect(html.querySelectorAll('.validaciones li').length).toBe(1);
    expect(html.querySelector('.sin-validaciones')).toBeNull();
  });

  it('con un rechazo temprano, omite tasa, score y siguiente paso sin romper la vista', () => {
    const rechazoTemprano = crearRespuestaRechazoTemprano();

    const html = pintarResultado(rechazoTemprano);

    const texto = obtenerTextoVisible(html);
    expect(texto).withContext('estado').toContain('RECHAZADO_FRAUDE');
    expect(texto).withContext('detalle de la validación').toContain('Documento reportado por fraude');
    expect(texto).withContext('tasa ausente').not.toContain('Tasa estimada');
    expect(texto).withContext('score ausente').not.toContain('Score del buró');
    expect(html.querySelector('.siguiente-paso')).toBeNull();
  });

  it('con la lista de validaciones vacía, pinta el aviso .sin-validaciones', () => {
    const sinValidaciones = crearRespuestaSinValidaciones();

    const html = pintarResultado(sinValidaciones);

    expect(html.querySelector('.sin-validaciones')?.textContent)
        .toContain('No se registraron validaciones para esta solicitud');
    expect(html.querySelector('.validaciones')).toBeNull();
  });
});
