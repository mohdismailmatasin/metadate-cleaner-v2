# Metadata Cleaner (aimc) - Privacy Inspector & Sanitizer
# Production container for local / self-hosted execution
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="mohdismailmatasin@gmail.com"
LABEL description="Metadata Cleaner - Privacy Inspector & Metadata Sanitizer"

WORKDIR /app

# Non-root user for security
RUN addgroup -S aimc && adduser -S aimc -G aimc

# Create cache and output storage directories
RUN mkdir -p /app/storage/input /app/storage/output && \
    chown -R aimc:aimc /app

USER aimc

# Environment variables
ENV AIMC_PORT=8080 \
    AIMC_STORAGE_DIR=/app/storage \
    AIMC_LOG_LEVEL=INFO

EXPOSE 8080

VOLUME ["/app/storage/input", "/app/storage/output"]

CMD ["echo", "AI Metadata Cleaner engine ready for CLI & REST operations."]
