# Use minimal Java 21 runtime (Alpine = ~150MB vs ~400MB for full image)
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user for security (capstone requirement)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the FAT JAR (built locally via maven-shade-plugin)
COPY target/distributed-file-engine-1.0-SNAPSHOT.jar app.jar

# Set ownership to non-root user
RUN chown appuser:appgroup app.jar

# Switch to non-root user (security best practice)
USER appuser

# Expose the gRPC port (configurable at runtime)
EXPOSE 8080

# Default environment variables (can be overridden with -e flags)
ENV SERVER_PORT=8080 \
    SERVER_HOST=0.0.0.0 \
    TEST_CLIENT_ID=default-client \
    CLIENT_ID=default-client

# Health check for orchestration readiness (optional but recommended)
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD nc -z localhost ${SERVER_PORT} || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]