ARG JDK_IMAGE=azul/zulu-openjdk:25-latest
ARG JRE_IMAGE=azul/zulu-openjdk:25-jre-headless-latest

FROM ${JDK_IMAGE} AS build
WORKDIR /workspace
COPY ./ ./
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon :modules:app:libertyPackage -x check

FROM ${JRE_IMAGE} AS runtime
WORKDIR /app
RUN groupadd --system --gid 1001 jakartaeeapi \
    && useradd --system --uid 1001 --gid jakartaeeapi --create-home jakartaeeapi

COPY --from=build --chown=jakartaeeapi:jakartaeeapi /workspace/modules/app/build/libs/jakarta-ee-api.jar app.jar

USER jakartaeeapi
EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /api/health HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q '\"UP\"' <&3"]

ENTRYPOINT ["java", "-jar", "app.jar"]
