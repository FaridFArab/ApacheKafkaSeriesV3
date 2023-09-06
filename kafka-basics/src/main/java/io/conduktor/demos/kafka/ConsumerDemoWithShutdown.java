package io.conduktor.demos.kafka;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

public class ConsumerDemoWithShutdown {
    public static final Logger logger = LoggerFactory.getLogger(ProducerDemo.class);
    public static void main(String[] args) {
        logger.info("I'm a Kafka Consumer");

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"mygroupid");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        // Change partition assignment strategy when a consumer joins/leaves consumer group
        properties.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());
        // Static assignment ID
        // properties.setProperty(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,"");

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(properties);

        // get a reference to the current thread
        final Thread mainThread = Thread.currentThread();

        // adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run() {
                logger.info("Detected a shutdown. let's exit by calling consumer.wakeup()");
                kafkaConsumer.wakeup();

                // join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try {
            // Single topic
            kafkaConsumer.subscribe(Collections.singletonList("DemoJava"));

            while (true){
                logger.info("Polling...");
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));

                for (ConsumerRecord<String, String> record: records){
                    logger.info("Key: " + record.key() + ", Value: " + record.value());
                    logger.info("Partition: " + record.partition() + ", Offset: " + record.offset());
                }
            }
        } catch (WakeupException e) {
            logger.error("Wake up exception !");
        }
        catch (Exception e){
            logger.error("Unexpected exception");
        }
        finally {
            kafkaConsumer.close();
            logger.info("The consumer is now gracefully closed");
        }
    }
}
