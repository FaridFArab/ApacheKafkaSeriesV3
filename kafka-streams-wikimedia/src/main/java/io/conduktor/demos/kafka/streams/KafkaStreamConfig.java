package io.conduktor.demos.kafka.streams;

import org.apache.kafka.common.serialization.Serdes;

public class KafkaStreamConfig
{
    public static final String applicationIdConfig = "wikimedia-stats-application";
    public static final String bootstrapServers = "localhost:9092";
    public static final Class<?> defaultKeyserdeclassconfig = Serdes.String().getClass();
    public static final Class<?> defaultValueSerdeClassConfig = Serdes.String().getClass();


    public String getBootstrapServers(){
        return bootstrapServers;
    }
    public String getApplicationIdConfig(){
        return applicationIdConfig;
    }
    public Class<?> getDefaultkeySerdeClassConfig(){
        return defaultKeyserdeclassconfig;
    }

    public Class<?> getDefaultvalueSerdeClassConfig(){
        return defaultValueSerdeClassConfig;
    }
}

