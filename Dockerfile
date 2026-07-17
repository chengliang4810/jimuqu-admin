FROM bellsoft/liberica-openjdk-debian:17.0.11-cds

LABEL maintainer="chengliang4810"

RUN mkdir -p /jimuqu/server/logs \
    /jimuqu/server/temp

WORKDIR /jimuqu/server

ENV SERVER_PORT=5320 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

COPY ./jimuqu-admin/target/jimuqu-admin.jar ./app.jar

ENTRYPOINT ["sh", "-c", "exec java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} -jar app.jar"]
