package io.conduktor.demos.kafka;

import org.apache.kafka.clients.producer.*;
import io.conduktor.demos.kafka.KafkaConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerDemoWithCallback {

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
        for(i = 1; i<=2; i++){
            String key = "id_" + i;
            ProducerRecord<String, String> record = new ProducerRecord<>("JavaDemoCallback", key, Integer.toString(i));
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    if (e == null)
                        logger.info("Received new metadata/ \n" +
                                "Topic: " + metadata.topic() + "\n" +
                                "Key: " + record.key() + "\n" +
                                "Partition: " + metadata.partition() + "\n" +
                                "Offset: " + metadata.offset() + "\n" +
                                "Timestamp: " + metadata.timestamp());
                    else
                        logger.error("Error while producing", e);
                }
            });
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }

            producer.flush();
        }
        producer.close();


    }
}
