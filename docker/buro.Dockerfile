# Imagen del buro de credito simulado. Java 21 (buro/pom.xml).
#
# Se construye con el contexto en buro/:
#   docker build -f docker/buro.Dockerfile -t creditvalidation-buro buro/
#
# El build va dentro de la imagen para que levantar el proyecto no dependa de
# tener Maven ni el JDK 21 instalados en la maquina.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# El pom primero: mientras no cambie, la capa de dependencias se reutiliza y el
# build no vuelve a bajar el repositorio entero.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario sin privilegios: nada dentro del contenedor necesita root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
