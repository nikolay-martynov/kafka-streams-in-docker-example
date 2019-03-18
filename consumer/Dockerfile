FROM openjdk:8u191-jre-alpine
ADD consumer.tar /opt/
ENTRYPOINT ["/opt/consumer/bin/consumer"]
CMD ["-c", "/opt/consumer/cfg/consumer.properties", "-t", "xml-topic", "-l", "/opt/consumer/cfg/log4j.xml"]
