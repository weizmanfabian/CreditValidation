# Imagen del motor de decision. Java 17 (motordedecision/pom.xml, D-023).
#
# Se construye con el contexto en motordedecision/:
#   docker build -f docker/motor.Dockerfile -t creditvalidation-motor motordedecision/
#
# Dentro del compose arranca con el perfil `docker`, que apunta a PostgreSQL
# por el nombre del servicio `db`; la URL del buro entra por BURO_URL.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# El pom primero: mientras no cambie, la capa de dependencias se reutiliza.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Usuario sin privilegios: nada dentro del contenedor necesita root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
