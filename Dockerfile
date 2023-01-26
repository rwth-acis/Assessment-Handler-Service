FROM openjdk:17-jdk-alpine


RUN apk add --update bash mysql-client apache-ant tzdata curl && rm -f /var/cache/apk/*
ENV TZ=Europe/Berlin

RUN addgroup -g 1000 -S las2peer && \
    adduser -u 1000 -S las2peer -G las2peer

COPY --chown=las2peer:las2peer . /src
WORKDIR /src

RUN chmod -R a+rwx /src
RUN chmod +x /src/docker-entrypoint.sh
# run the rest as unprivileged user
USER las2peer
RUN chmod +x ./gradlew && ./gradlew build --exclude-task test
RUN chmod +x /src/docker-entrypoint.sh

EXPOSE $HTTP_PORT
EXPOSE $HTTPS_PORT
EXPOSE $LAS2PEER_PORT
ENTRYPOINT ["/src/docker-entrypoint.sh"]
