# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
# Builds the React SPA and packages the Spring Boot fat JAR in one step.
# The frontend-maven-plugin runs `npm install` and `npm run build` during the
# Maven `generate-resources` phase, so no separate Node container is needed.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

# Cache Maven dependencies first
COPY pom.xml ./
# If the project contains a .mvn wrapper directory, copy it. If not, this
# COPY will simply copy an empty/non-existent path and cause a build failure
# only if the directory truly doesn't exist in the build context. The repo
# typically contains `.mvn/` so this is safe.
COPY .mvn/ .mvn/

# Copy full source tree
COPY src/ src/
COPY frontend/ frontend/

# Build the fat JAR (frontend is compiled by frontend-maven-plugin)
RUN mvn -B clean package -DskipTests

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime
# Slim JRE image; only the fat JAR is copied from the build stage.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre AS runtime

# Non-root user for security
RUN addgroup --system --gid 1001 appgroup && \
    adduser  --system --uid 1001 --ingroup appgroup appuser

WORKDIR /app

COPY --from=build /workspace/target/automated-fuel-quota-0.0.1-SNAPSHOT.jar app.jar

USER appuser


# ── Runtime configuration ──────────────────────────────────────────────────
# IMPORTANT: Do NOT bake runtime configuration or secrets into the image.
# All sensitive/runtime configuration (database credentials, JWT secret,
# OTP secrets, quota values, management settings, etc.) MUST be supplied by
# Kubernetes (ConfigMap for non-sensitive values and Secrets for sensitive
# values) as environment variables or mounted files. This image only selects
# the `k8s` Spring profile so the application will read configuration from
# environment variables defined by the Kubernetes deployment.
ENV SPRING_PROFILES_ACTIVE=k8s

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

