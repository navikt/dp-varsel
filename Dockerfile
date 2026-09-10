FROM cgr.dev/chainguard/jre:latest@sha256:78e7fb5ae068709a02f7f25017d2464e50a2bf803f93a1be4ea0b9bd2443d449

COPY build/libs/*.jar /app/

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

CMD ["-jar", "app.jar"]
