FROM openjdk:8u191-jre-alpine
ADD producer.tar /opt/
ENTRYPOINT ["/opt/producer/bin/producer"]
CMD ["-c", "/opt/producer/cfg/producer.properties", "-t", "json-topic", "-l", "/opt/producer/cfg/log4j.xml"]
