package example

import groovy.cli.commons.CliBuilder
import groovy.cli.commons.OptionAccessor
import groovy.json.JsonOutput
import org.apache.commons.cli.Option
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

class ExampleProducer {

    static void main(String[] args) {
        CliBuilder cli = new CliBuilder(usage: "producer <options>")
        cli << Option.builder("c")
                .argName("property file")
                .desc("producer configuration")
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
        cli << Option.builder("d")
                .argName("seconds")
                .desc("delay between sending each message, can be 0, default is 1 second")
                .hasArg(true)
                .longOpt("delay")
                .numberOfArgs(1)
                .build()
        cli << Option.builder("n")
                .argName("count")
                .desc("messages to send, default is until stopped")
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
        long delay = TimeUnit.SECONDS.toMillis(options.d ? options.d as Integer : 1)
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
        props.put("key.serializer", org.apache.kafka.common.serialization.StringSerializer.name)
        props.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer.name)
        Producer<String, String> producer = new KafkaProducer<>(props)
        try {
            (1..count).each {
                String message = JsonOutput.toJson([number: it])
                LoggerFactory.getLogger(ExampleProducer).info("Sending message #$it")
                producer.send(new ProducerRecord<String, String>(topic, it as String, message))
                if (delay) {
                    Thread.sleep(delay)
                }
            }
        } finally {
            producer.close()
        }
    }

}
