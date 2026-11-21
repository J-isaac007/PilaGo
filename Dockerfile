# PilaGo on Render. Two stages: build the jar with the Maven wrapper, then run it on a small JRE image.

# ── Build ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Download dependencies first, so they are cached when only the source changes
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q -DskipTests package && cp target/pilago-*.jar app.jar

# ── Run ──────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 1001 pilago
COPY --from=build /app/app.jar app.jar
USER pilago

# Render's free plan has 512 MB of memory; Render sets PORT, which application-prod.properties reads.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
