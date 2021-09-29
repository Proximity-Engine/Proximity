FROM gradle:jdk16 AS compiler
WORKDIR /home/gradle/project
COPY build.gradle gradlew settings.gradle ./
COPY src src
RUN gradle shadowJar

FROM adoptopenjdk:16-jre-hotspot AS runner
WORKDIR /app
COPY --from=compiler /home/gradle/project/build/**/*.jar proximity.jar
CMD ["java", "-jar", "proximity.jar"]
