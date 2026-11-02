FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:0ee166df2ef38f8674f1cd8aa6e7178992eb9bcf23a418799c6bb56ce486098f

COPY build/libs/*.jar /app/

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

CMD ["-jar", "app.jar"]
