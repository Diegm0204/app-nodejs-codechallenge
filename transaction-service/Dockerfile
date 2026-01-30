# Build multi-etapa para optimizar tamaño de imagen
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copiar archivos gradle
COPY settings.gradle build.gradle ./
COPY shared-domain/build.gradle shared-domain/build.gradle
COPY transaction-service/build.gradle transaction-service/build.gradle

# Copiar código fuente
COPY shared-domain/src shared-domain/src
COPY transaction-service/src transaction-service/src

# Compilar la aplicación
RUN gradle :transaction-service:build -x test --no-daemon

# Etapa de runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar el jar compilado desde etapa builder
COPY --from=builder /app/transaction-service/build/libs/*.jar app.jar

# Agregar health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Exponer puerto
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
