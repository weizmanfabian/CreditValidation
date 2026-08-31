import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { RUTA_SOLICITUDES, SolicitudCreditoService } from './solicitud-credito.service';
import { SolicitudCreditoResponse } from '../models/solicitud-credito';
import { crearRespuestaAprobada, crearSolicitudValida } from '../testing/fixtures-solicitud';

describe('SolicitudCreditoService', () => {
  let servicio: SolicitudCreditoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(SolicitudCreditoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('radicar: hace POST a /api/solicitudes con el cuerpo y devuelve la respuesta tipada', () => {
    const solicitud = crearSolicitudValida();
    let respuestaRecibida: SolicitudCreditoResponse | undefined;

    servicio.radicar(solicitud).subscribe((respuesta) => (respuestaRecibida = respuesta));

    const peticion = httpTesting.expectOne(RUTA_SOLICITUDES);
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual(solicitud);
    peticion.flush(crearRespuestaAprobada());
    expect(respuestaRecibida).toEqual(crearRespuestaAprobada());
  });

  it('radicar: un 400 del motor se propaga como error del observable', () => {
    let errorRecibido: HttpErrorResponse | undefined;

    servicio.radicar(crearSolicitudValida()).subscribe({
      error: (error: HttpErrorResponse) => (errorRecibido = error),
    });

    httpTesting.expectOne(RUTA_SOLICITUDES).flush(
      { message: 'Error en los datos proporcionados' },
      { status: 400, statusText: 'Bad Request' },
    );
    expect(errorRecibido?.status).toBe(400);
  });
});
