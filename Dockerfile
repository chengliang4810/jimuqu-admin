# 贝尔实验室 Spring 官方推荐镜像 JDK下载地址 https://bell-sw.com/pages/downloads/
#FROM bellsoft/liberica-openjdk-debian:17.0.11-cds
FROM docker.1panel.live/bellsoft/liberica-openjdk-debian:17.0.11-cds
#FROM findepi/graalvm:java17-native

LABEL maintainer="chengliang4810"

RUN mkdir -p /jimuqu/server/logs \
    /jimuqu/server/temp

WORKDIR /jimuqu/server

ENV SERVER_PORT=5320 LANG=C.UTF-8 LC_ALL=C.UTF-8 JAVA_OPTS=""

EXPOSE ${SERVER_PORT}

ADD ./jimuqu-admin/target/jimuqu-admin.jar ./app.jar

ENTRYPOINT java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${SERVER_PORT} \
           -XX:+HeapDumpOnOutOfMemoryError -XX:+UseZGC ${JAVA_OPTS} \
           -jar app.jar
