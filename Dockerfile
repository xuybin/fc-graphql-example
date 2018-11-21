FROM 8-jdk-alpine

COPY .  /application
RUN apk add --update unzip && \
    cd /application/ && chmod +x ./gradlew && ./gradlew  assemble && cd / && \
    unzip /application/build/libs/*.jar -d /deployments && chmod 755 /deployments/bin/* && \
    apk del unzip && \
    rm -rf /tmp/*  /var/cache/apk/* && \
    find ~/.gradle  /application -delete

CMD [ "/deployments/run-in-docker" ]
EXPOSE 8080


