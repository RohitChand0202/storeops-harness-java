FROM maven:3.9.16-eclipse-temurin-17-alpine AS build
WORKDIR /build

COPY pom.xml ./
COPY .harness/checks ./.harness/checks
COPY src ./src
RUN mvn -B clean verify -Pharness-gates

FROM eclipse-temurin:17-jre-alpine-3.24
WORKDIR /app
RUN addgroup -S storeops && adduser -S -G storeops storeops
COPY --from=build --chown=storeops:storeops /build/target/storeops-0.0.1-SNAPSHOT.jar /app/app.jar
USER storeops
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
