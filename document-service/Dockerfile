# Stage 1: Build
FROM eclipse-temurin:20-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B || true
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:20-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app/uploads
USER appuser
COPY --from=builder /app/target/*.jar app.jar
VOLUME /app/uploads
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
