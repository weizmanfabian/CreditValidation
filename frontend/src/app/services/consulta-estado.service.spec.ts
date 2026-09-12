import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ConsultaEstadoService } from './consulta-estado.service';
import { EstadoDeSolicitud } from '../models/estado-solicitud';
import { crearConsultaDeEstado, crearSolicitudesDelDocumento } from '../testing/fixtures-solicitud';

// La ruta se escribe literal, no compuesta a partir de las variables del
// servicio: es la unica forma de que el test se entere si cambia el orden del
// tipo y el numero de documento (D-053).
const RUTA_ESPERADA = '/api/solicitudes/CC/1234567890';

describe('ConsultaEstadoService', () => {
  let servicio: ConsultaEstadoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(ConsultaEstadoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('consultarPorDocumento: hace GET a /api/solicitudes/CC/1234567890 y devuelve la lista tipada', () => {
    let solicitudesRecibidas: EstadoDeSolicitud[] | undefined;

    servicio.consultarPorDocumento(crearConsultaDeEstado())
        .subscribe((solicitudes) => (solicitudesRecibidas = solicitudes));

    const peticion = httpTesting.expectOne(RUTA_ESPERADA);
    expect(peticion.request.method).toBe('GET');
    peticion.flush(crearSolicitudesDelDocumento());
    expect(solicitudesRecibidas).toEqual(crearSolicitudesDelDocumento());
  });

  it('consultarPorDocumento: un documento sin solicitudes devuelve la lista vacía', () => {
    let solicitudesRecibidas: EstadoDeSolicitud[] | undefined;

    servicio.consultarPorDocumento(crearConsultaDeEstado())
        .subscribe((solicitudes) => (solicitudesRecibidas = solicitudes));

    httpTesting.expectOne(RUTA_ESPERADA).flush([]);
    expect(solicitudesRecibidas).toEqual([]);
  });
});
