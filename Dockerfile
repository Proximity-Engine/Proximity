FROM gradle:jdk16 AS compiler
WORKDIR /home/gradle

COPY gradle build.gradle gradlew settings.gradle ./
COPY src src
RUN gradle --no-daemon shadowJar

FROM adoptopenjdk:16-jre-hotspot AS runner
WORKDIR /app
COPY --from=compiler /home/gradle/build/**/*.jar proximity.jar

CMD ["--cards=cards.txt", "--template=template.zip"]
ENTRYPOINT ["java", "-jar", "proximity.jar"]
