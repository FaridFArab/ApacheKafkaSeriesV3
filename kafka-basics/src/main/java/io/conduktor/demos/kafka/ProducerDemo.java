package io.conduktor.demos.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.Properties;

public class ProducerDemo {

    public static final Logger logger = LoggerFactory.getLogger(ProducerDemo.class);
    public static void main(String[] args) {
//        System.out.println("Hello Kafka");
        logger.info("I'm a Kafka Producer");

        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        int i;
        for(i = 1; i<=10; i++){

            // Produced without key
            ProducerRecord<String, String> record = new ProducerRecord<>("JavaDemo", Integer.toString(i));

            producer.send(record);
            producer.flush();
        }
        producer.close();


    }
}
