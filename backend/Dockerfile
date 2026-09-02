# --- Build stage ---
FROM maven:3.9-eclipse-temurin-23 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:23-jre-alpine
WORKDIR /app
COPY --from=build /app/target/shopsphere-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]