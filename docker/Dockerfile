FROM gradle:jdk16 AS compiler
WORKDIR /home/gradle

COPY gradle build.gradle gradlew settings.gradle ./
COPY src src
RUN gradle --no-daemon shadowJar

FROM adoptopenjdk:16-jre-hotspot AS runner
WORKDIR /app

ARG USER_ID=1000
ARG GROUP_ID=1000
RUN addgroup --gid $GROUP_ID user
RUN adduser --disabled-password --gecos '' --uid $USER_ID --gid $GROUP_ID user
USER user

COPY --chown=user --from=compiler /home/gradle/build/**/*.jar proximity.jar
COPY --chown=user docker/entrypoint.sh entrypoint.sh

CMD ["--template=normal", "--cards=cardslist"]
ENTRYPOINT ["./entrypoint.sh"]
