// El navegador no resuelve nombres de servicio de Docker y el motor no publica
// CORS: el frontend llama rutas relativas /api y este proxy del dev-server las
// reenvía al motor. MOTOR_URL permite apuntar al nombre de servicio del
// compose (feature 19); sin ella, el motor local en 8080.
const objetivoMotor = process.env['MOTOR_URL'] ?? 'http://localhost:8080';

module.exports = {
  '/api': {
    target: objetivoMotor,
    changeOrigin: true,
  },
};
