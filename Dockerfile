FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# npm runs Jest only (static JS ships as-is from src/main/resources), so the
# frontend plugin is skipped together with the backend tests
RUN apk add --no-cache maven && \
    mvn package -DskipTests -Dskip.installnodenpm -Dskip.npm

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.war app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.war"]
