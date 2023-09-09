package io.conduktor.demos.kafka.streams;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Properties;

public class WikimediaStreamsApp {
    private static final Logger logger = LoggerFactory.getLogger(WikimediaStreamsApp.class);
    private static final String INPUT_TOPIC = "wikimedia.recentchange";
    private static final Properties properties;

    static KafkaStreamConfig kafkaStreamConfig = new KafkaStreamConfig();

    static {
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG,kafkaStreamConfig.getApplicationIdConfig());
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,kafkaStreamConfig.getBootstrapServers());
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,kafkaStreamConfig.getDefaultkeySerdeClassConfig());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,kafkaStreamConfig.getDefaultvalueSerdeClassConfig());
    }
    public static void main(String[] args) {
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String , String> changeJsonStream = builder.stream(INPUT_TOPIC);
//        BotCountStreamBuilder botCOuntStreamBuilder = new BotCountStreamBuilder(changeJsonStream);
    }
}
