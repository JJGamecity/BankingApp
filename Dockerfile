FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src/main src/main
RUN sh ./mvnw -B -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/*.jar /app/banking.jar
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod,vercel
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/banking.jar"]
