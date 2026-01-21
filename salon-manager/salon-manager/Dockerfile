# Multi-stage build dla optymalizacji rozmiaru obrazu

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Kopiuj pliki Maven
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Pobierz zależności (cachowanie warstwy)
RUN ./mvnw dependency:go-offline -B

# Kopiuj kod źródłowy
COPY src src

# Zbuduj aplikację (pomijamy testy dla szybszego budowania)
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Utwórz użytkownika non-root dla bezpieczeństwa
RUN addgroup -S spring && adduser -S spring -G spring

# Utwórz katalog uploads z odpowiednimi uprawnieniami
RUN mkdir -p /app/uploads && chown -R spring:spring /app/uploads

# Kopiuj zbudowany JAR z poprzedniego stage'a
COPY --from=builder /app/target/*.jar app.jar

# Ustaw właściciela plików
RUN chown spring:spring /app/app.jar

USER spring:spring

# Eksponuj port
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Uruchom aplikację z optymalizacjami JVM
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
