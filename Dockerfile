FROM eclipse-temurin:8u504-b01-jdk AS builder

ENV GRADLE_VERSION=4.10.3
ENV GRADLE_HOME=/opt/gradle
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip ca-certificates \
    && curl -fsSL \
       "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
       -o /tmp/gradle.zip \
    && mkdir -p /opt \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv "/opt/gradle-${GRADLE_VERSION}" "${GRADLE_HOME}" \
    && rm /tmp/gradle.zip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /home/gradle

USER root
COPY . .
RUN gradle --no-daemon build

FROM eclipse-temurin:8u504-b01-jre

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"

WORKDIR /app

COPY --from=builder /home/gradle/build/deps/external/*.jar /app/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /app/
COPY --from=builder /home/gradle/build/libs/fint-faks-adapter-*.jar /app/fint-faks-adapter.jar

ENTRYPOINT ["java", "-jar", "/app/fint-faks-adapter.jar"]
