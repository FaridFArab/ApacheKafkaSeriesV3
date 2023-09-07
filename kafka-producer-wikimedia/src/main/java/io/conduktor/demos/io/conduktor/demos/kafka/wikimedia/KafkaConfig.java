package io.conduktor.demos.io.conduktor.demos.kafka.wikimedia;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaConfig {
    static String bootstrapServers = "localhost:9092";
    static String valueSerializer = StringSerializer.class.getName();
    static String keySerializer = StringSerializer.class.getName();
    static String valueDeserializer = StringDeserializer.class.getName();
    static String keyDeserializer = StringDeserializer.class.getName();

    public String getBootstrapServers(){
        return bootstrapServers;
    }

    public String getKeySerializer(){
        return keySerializer;
    }

    public String getValueSerializer(){
        return valueSerializer;
    }
}