FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY backend/pom.xml backend/
COPY backend/settings.xml /root/.m2/settings.xml
RUN mvn -B -f pom.xml -pl backend dependency:go-offline
COPY backend/src backend/src
RUN mvn -B -f pom.xml -pl backend -Pdocker package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/backend/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
