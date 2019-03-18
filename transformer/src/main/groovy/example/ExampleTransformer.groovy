package example

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.apache.commons.cli.Option
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.ValueMapper
import org.slf4j.LoggerFactory

import java.util.concurrent.CountDownLatch

class ExampleTransformer {

    static List<String> toXml(String json) {
        LoggerFactory.getLogger(ExampleTransformer).info("Translating $json")
        JsonSlurper slurper = new JsonSlurper()
        Map<String, ?> record = slurper.parseText(json) as Map
        Writer writer = new StringWriter()
        MarkupBuilder xml = new MarkupBuilder(writer)
        xml.message(value: record["number"])
        [writer.toString()]
    }

    static void main(String[] args) {
        CliBuilder cli = new CliBuilder(usage: "transformer <options>")
        cli << Option.builder("c")
                .argName("property file")
                .desc("transformer configuration")
                .hasArg(true)
                .longOpt("config")
                .numberOfArgs(1)
                .required()
                .build()
        cli << Option.builder("s")
                .argName("topic name")
                .desc("name of source topic")
                .hasArg(true)
                .longOpt("source-topic")
                .numberOfArgs(1)
                .required()
                .build()
        cli << Option.builder("t")
                .argName("topic name")
                .desc("name of target topic")
                .hasArg(true)
                .longOpt("target-topic")
                .numberOfArgs(1)
                .required()
                .build()
        cli << Option.builder("l")
                .argName("log4j 2 config file")
                .desc("logging configuration")
                .hasArg(true)
                .longOpt("logging")
                .numberOfArgs(1)
                .build()
        OptionAccessor options = cli.parse(args)
        if (!options) {
            System.exit(-1)
        }
        String config = options.c
        String sourceTopic = options.s
        String targetTopic = options.t
        if (options.l) {
            System.properties["log4j.configurationFile"] = options.l
        }
        Properties props = new Properties()
        new File(config).tap {
            assert it.file && it.canRead(): "$config is not a readable file"
            withReader { Reader reader ->
                props.load(reader)
            }
        }
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass())
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass())
        StreamsBuilder builder = new StreamsBuilder()
        builder.stream(sourceTopic).flatMapValues({ toXml(it as String) } as ValueMapper).to(targetTopic)
        Topology topology = builder.build()
        KafkaStreams streams = new KafkaStreams(topology, props)
        CountDownLatch latch = new CountDownLatch(1)
        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            void run() {
                streams.close()
                latch.countDown()
            }
        })
        try {
            streams.start()
            latch.await()
        } catch (Throwable e) {
            System.exit(1)
        }
        System.exit(0)
    }

}
