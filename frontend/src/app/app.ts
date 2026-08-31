import { Component, signal } from '@angular/core';

import { ConsultaEstado } from './components/consulta-estado/consulta-estado';
import { FormularioSolicitud } from './components/formulario-solicitud/formulario-solicitud';

export type PantallaActiva = 'solicitud' | 'consulta';

@Component({
  selector: 'app-root',
  imports: [ConsultaEstado, FormularioSolicitud],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {

  protected readonly pantalla = signal<PantallaActiva>('solicitud');

  protected mostrarPantalla(pantalla: PantallaActiva): void {
    this.pantalla.set(pantalla);
  }
}
