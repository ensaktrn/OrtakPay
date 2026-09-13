# Stage 1: build the fat jar and split it into layertools layers.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Tests already run in Faz 0-6 CI; re-running them on every image build only
# adds time (and would require Postgres/RabbitMQ to be reachable at build time).
RUN mvn -B clean package -DskipTests

# --layers (bare flag) splits the jar into dependencies/spring-boot-loader/
# snapshot-dependencies/application directories; --launcher keeps the loader
# classes needed to run via JarLauncher instead of `java -jar`.
RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /build/extracted

# Stage 2: runtime image - just a JRE, the extracted layers, and a non-root user.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S appuser && adduser -S appuser -G appuser
WORKDIR /application

# Least-to-most-frequently-changing layers first, so unrelated app-code changes
# only invalidate the last COPY instead of re-pulling every dependency jar.
COPY --from=build --chown=appuser:appuser /build/extracted/dependencies/ ./
COPY --from=build --chown=appuser:appuser /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=appuser:appuser /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=appuser:appuser /build/extracted/application/ ./

USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
