import { Component } from '@angular/core';

import { FormularioSolicitud } from './components/formulario-solicitud/formulario-solicitud';

@Component({
  selector: 'app-root',
  imports: [FormularioSolicitud],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
