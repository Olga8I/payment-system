FROM gradle:8.14.3-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=builder --chown=spring:spring /app/build/libs/*.jar app.jar

RUN mkdir -p /app/packets
ENTRYPOINT ["java", "-jar", "app.jar"]