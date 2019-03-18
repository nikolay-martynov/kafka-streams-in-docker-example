package example

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import org.apache.commons.cli.Option
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

import java.time.Duration

class ExampleConsumer {

    static void main(String[] args) {
        CliBuilder cli = new CliBuilder(usage: "consumer <options>")
        cli << Option.builder("c")
                .argName("property file")
                .desc("consumer configuration")
                .hasArg(true)
                .longOpt("config")
                .numberOfArgs(1)
                .required()
                .build()
        cli << Option.builder("t")
                .argName("topic name")
                .desc("name of topic")
                .hasArg(true)
                .longOpt("topic")
                .numberOfArgs(1)
                .required()
                .build()
        cli << Option.builder("n")
                .argName("count")
                .desc("messages to receive, default is until stopped")
                .hasArg(true)
                .longOpt("count")
                .type(Number)
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
        String topic = options.t
        int count = options.n ? options.n as Integer : Integer.MAX_VALUE
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
        props.put("key.deserializer", org.apache.kafka.common.serialization.StringDeserializer.name)
        props.put("value.deserializer", org.apache.kafka.common.serialization.StringDeserializer.name)
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)
        int received = 0
        try {
            consumer.subscribe([topic])
            while (!Thread.currentThread().interrupted && received < count) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1))
                records.each {
                    received++
                    LoggerFactory.getLogger(ExampleConsumer).info("Received message #$received: $it")
                }
            }
        } finally {
            consumer.close()
        }
    }

}
