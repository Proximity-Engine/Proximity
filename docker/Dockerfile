FROM gradle:jdk16 AS compiler
WORKDIR /home/gradle

COPY gradle build.gradle gradlew settings.gradle ./
COPY src src
RUN gradle --no-daemon shadowJar

FROM adoptopenjdk:16-jre-hotspot AS runner
WORKDIR /app

COPY --from=compiler /home/gradle/build/**/*.jar proximity.jar
COPY docker/entrypoint.sh entrypoint.sh

CMD ["--template=normal", "--cards=cardslist"]
ENTRYPOINT ["./entrypoint.sh"]
