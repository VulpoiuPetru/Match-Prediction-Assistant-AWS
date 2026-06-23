#FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
#WORKDIR /app
#COPY pom.xml .
#RUN mvn dependency:go-offline -q
#COPY src ./src
#RUN mvn clean package -DskipTests -q
#
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY --from=build /app/target/match-predictor-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]
FROM public.ecr.aws/docker/library/maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/match-predictor-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]