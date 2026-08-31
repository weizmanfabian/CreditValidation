const { join } = require('node:path');
const { existsSync } = require('node:fs');

// El arnes (init.sh, paso 5) verifica el frontend con la linea exacta
// `npm test -- --watch=false --browsers=ChromeHeadless`. El lanzador
// ChromeHeadless acepta cualquier binario Chromium via CHROME_BIN, asi que
// aqui se resuelve el primero disponible (Chrome primero, Edge como respaldo)
// para que esa linea funcione tambien en maquinas sin Chrome (D-057).
const CANDIDATOS_CHROMIUM = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  process.env.LOCALAPPDATA
    ? join(process.env.LOCALAPPDATA, 'Google', 'Chrome', 'Application', 'chrome.exe')
    : '',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  '/usr/bin/google-chrome',
  '/usr/bin/chromium',
  '/usr/bin/chromium-browser',
].filter(Boolean);

if (!process.env.CHROME_BIN) {
  const binario = CANDIDATOS_CHROMIUM.find(existsSync);
  if (binario) {
    process.env.CHROME_BIN = binario;
  }
}

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
    ],
    jasmineHtmlReporter: {
      suppressAll: true,
    },
    coverageReporter: {
      dir: join(__dirname, 'coverage', 'frontend'),
      subdir: '.',
      reporters: [{ type: 'html' }, { type: 'text-summary' }],
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['ChromeHeadless'],
    restartOnFileChange: true,
  });
};
