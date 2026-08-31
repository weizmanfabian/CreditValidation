import { AbstractControl } from '@angular/forms';

/**
 * Devuelve el mensaje del primer error del control que tenga texto declarado, o
 * `null` si el control es válido o el usuario aún no lo ha tocado.
 *
 * Lo comparten los dos formularios: el de radicación y el de consulta de estado.
 */
export function obtenerMensajeDeError(control: AbstractControl,
                                      mensajesDelCampo: Record<string, string>): string | null {
  if (control.valid || !(control.touched || control.dirty)) {
    return null;
  }
  const claveError = Object.keys(control.errors ?? {}).find((clave) => clave in mensajesDelCampo);
  return claveError === undefined ? null : mensajesDelCampo[claveError];
}
