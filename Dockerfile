FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -Dmaven.test.skip=true package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system bank && useradd --system --gid bank bank
COPY --from=build /build/target/minibank-transfers-service-1.0.0.jar app.jar
USER bank
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx192m"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
