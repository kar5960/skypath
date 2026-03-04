# Stage 1: Build the Quarkus JAR
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2: Minimal JRE runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /build/target/quarkus-app/lib/ lib/
COPY --from=builder /build/target/quarkus-app/*.jar ./
COPY --from=builder /build/target/quarkus-app/app/ app/
COPY --from=builder /build/target/quarkus-app/quarkus/ quarkus/

EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx256m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar quarkus-run.jar"]