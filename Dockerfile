FROM maven:3.9-eclipse-temurin-17 as builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=builder /app/target/*jar app.jar
EXPOSE 8081
ENTRYPOINT [ "sh", "-c", "java -jar app.jar" ]