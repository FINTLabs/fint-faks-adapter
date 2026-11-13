FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /home/gradle

USER root
COPY . .
RUN ./gradlew --no-daemon build

FROM eclipse-temurin:17-jre-jammy

ENV JAVA_TOOL_OPTIONS="-XX:+ExitOnOutOfMemoryError"

WORKDIR /app

COPY --from=builder /home/gradle/build/deps/external/*.jar /app/
COPY --from=builder /home/gradle/build/deps/fint/*.jar /app/
COPY --from=builder /home/gradle/build/libs/fint-faks-adapter-*.jar /app/fint-faks-adapter.jar

ENTRYPOINT ["java", "-jar", "/app/fint-faks-adapter.jar"]
