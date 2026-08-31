import { FormControl } from '@angular/forms';

import { validarPlazoPermitido } from './plazo-permitido';

describe('validarPlazoPermitido', () => {
  const casosValidos: (number | null)[] = [null, 12, 24, 36, 48];

  for (const plazo of casosValidos) {
    it(`acepta ${plazo} sin error`, () => {
      const control = new FormControl<number | null>(plazo);

      expect(validarPlazoPermitido(control)).toBeNull();
    });
  }

  const casosInvalidos = [0, 6, 13, 60];

  for (const plazo of casosInvalidos) {
    it(`rechaza ${plazo} con el error plazoPermitido`, () => {
      const control = new FormControl<number | null>(plazo);

      expect(validarPlazoPermitido(control)).toEqual({ plazoPermitido: { plazoRecibido: plazo } });
    });
  }
});
