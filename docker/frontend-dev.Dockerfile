# Imagen de DESARROLLO del frontend: ng serve con recarga, no un build de produccion.
#
# Se construye con el contexto en frontend/ (el .dockerignore del modulo excluye
# node_modules y artefactos):
#   docker build -f docker/frontend-dev.Dockerfile -t creditvalidation-frontend-dev frontend/
#   docker run --rm -p 4200:4200 creditvalidation-frontend-dev
FROM node:24-alpine

WORKDIR /app

# Las dependencias primero, para que la capa se reutilice mientras no cambie el lock.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

EXPOSE 4200

# --host 0.0.0.0 expone el dev-server fuera del contenedor; --poll 2000 porque
# los eventos de archivos no cruzan un bind mount desde Windows.
CMD ["npm", "start", "--", "--host", "0.0.0.0", "--poll", "2000"]
