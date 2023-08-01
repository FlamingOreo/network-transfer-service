#build docker image
FROM hc-eu-west-aws-artifactory.cloud.health.ge.com/docker-eml-release/corretto-11.0.17:sles15-sp3 as corretto
ARG VERSION=1.0.0
WORKDIR /usr/g/applications/network-transfer-service
COPY ./network-transfer-service-rest/target/network-transfer-service-rest-1.0.0.jar ./network-transfer-service.jar
COPY  ./dev/scripts/entrypoint.sh ./entrypoint.sh
EXPOSE 3002
RUN chmod +x ./entrypoint.sh
#ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "service.jar"]
#ENTRYPOINT ["java", "-Xms8m", "-Xmx40m", "-XX:+UseSerialGC", "-XX:CICompilerCount=2", "-Xss228k","-XX:MaxRAM=64m", "-XX:NativeMemoryTracking=summary", "-XX:InitialCodeCacheSize=2m", "-XX:ReservedCodeCacheSize=8m","-XX:MaxDirectMemorySize=8m", "-XX:MaxMetaspaceSize=60m", "-XX:MinMetaspaceFreeRatio=40", "-XX:MaxMetaspaceFreeRatio=80", "-jar", "service.jar"]
ENTRYPOINT ["./entrypoint.sh"]
