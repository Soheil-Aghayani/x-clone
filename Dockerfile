FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package dependency:copy-dependencies \
    -DincludeScope=runtime \
    -DoutputDirectory=/workspace/runtime-libs

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 10001 --create-home xclone
COPY --from=build /workspace/target/TwitterClone-1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /workspace/runtime-libs /app/lib

ENV PORT=8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -Dfile.encoding=UTF-8"
EXPOSE 8080

USER xclone
CMD ["sh", "-c", "exec java -cp '/app/app.jar:/app/lib/*' server.network.server"]
