## Multi-stage build that bundles Angular frontend and Spring Boot backend
## into a single container for Render.

### 1) Build Angular frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app

# Copy Angular project
COPY Frontend/InstaMetrics/package*.json ./frontend/
WORKDIR /app/frontend
RUN npm ci

# Copy the rest of the Angular source and build (production build)
COPY Frontend/InstaMetrics/ /app/frontend/
RUN npm run build

### 2) Build Spring Boot backend and embed built frontend
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace

# Copy backend Maven project
COPY Backend/InstaMetrics/pom.xml Backend/InstaMetrics/pom.xml
COPY Backend/InstaMetrics/src Backend/InstaMetrics/src

# Copy built Angular assets into Spring Boot static resources so they are
# served by the same backend application.
RUN mkdir -p Backend/InstaMetrics/src/main/resources/static
# Angular 17+ default output is dist/InstaMetrics/browser when using the new builder
# This copies the browser build into Spring Boot's static folder.
COPY --from=frontend-build /app/frontend/dist/InstaMetrics/browser/ ./Backend/InstaMetrics/src/main/resources/static/
## TODO: if you later change the Angular project name or output structure,
## update the path above accordingly.

# Package Spring Boot application (fat JAR)
RUN mvn -f Backend/InstaMetrics/pom.xml -DskipTests package

### 3) Runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from backend-build stage
COPY --from=backend-build /workspace/Backend/InstaMetrics/target/InstaMetrics-0.0.1-SNAPSHOT.jar app.jar

# TODO: configure environment variables on Render (e.g. SERVER_PORT, ALLOWED_ORIGIN, any DB/API keys)
# and wire them in your Spring Boot configuration instead of hardcoding values.

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

