import { AbstractControl, ValidationErrors } from '@angular/forms';

import { PLAZOS_PERMITIDOS } from '../models/reglas-solicitud-credito';

export function validarPlazoPermitido(control: AbstractControl<number | null>): ValidationErrors | null {
  const plazo = control.value;
  if (plazo === null || PLAZOS_PERMITIDOS.includes(plazo)) {
    return null;
  }
  return { plazoPermitido: { plazoRecibido: plazo } };
}
