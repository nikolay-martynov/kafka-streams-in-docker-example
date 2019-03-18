FROM openjdk:8u191-jre-alpine
ADD transformer.tar /opt/
ENTRYPOINT ["/opt/transformer/bin/transformer"]
CMD ["-c", "/opt/transformer/cfg/transformer.properties", "-s", "json-topic", "-t", "xml-topic", "-l", "/opt/transformer/cfg/log4j.xml"]
