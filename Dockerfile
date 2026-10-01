FROM eclipse-temurin:8-jre

WORKDIR /app

RUN useradd --system --uid 10001 --create-home --home-dir /home/app app \
    && mkdir -p /app/data \
    && chown -R app:app /app /home/app

COPY target/push-*.jar /app/app.jar

USER 10001

EXPOSE 8085

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
