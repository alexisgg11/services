FROM maven:3.9.11-eclipse-temurin-21 AS maven-distribution
FROM eclipse-temurin:25-jdk-jammy AS build
COPY --from=maven-distribution /usr/share/maven /usr/share/maven
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="/usr/share/maven/bin:${PATH}"
WORKDIR /build
COPY abastecimiento/pom.xml ./pom.xml
COPY abastecimiento/src ./src
RUN mvn -B package dependency:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /build/target/abastecimiento-1.0-SNAPSHOT.jar ./app.jar
COPY --from=build /build/target/dependency ./lib
ENV PORT=8080
EXPOSE 8080
USER 10001
CMD ["java", "-XX:MaxRAMPercentage=70", "-cp", "app.jar:lib/*", "com.alexis.App"]
