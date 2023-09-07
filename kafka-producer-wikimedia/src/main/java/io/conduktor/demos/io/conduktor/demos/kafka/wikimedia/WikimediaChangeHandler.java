package io.conduktor.demos.io.conduktor.demos.kafka.wikimedia;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikimediaChangeHandler implements EventHandler {

    public static final Logger logger = LoggerFactory.getLogger(WikimediaChangeHandler.class);
    KafkaProducer<String, String> kafkaProducer;
    String topic;

    public WikimediaChangeHandler(KafkaProducer<String, String> kafkaProducer, String topic){
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;

    }
    @Override
    public void onOpen() {
        // do nothing
    }

    @Override
    public void onClosed() throws Exception {
        kafkaProducer.close();

    }

    @Override
    public void onMessage(String event, MessageEvent messageEvent) {
        logger.info(messageEvent.getData());
        kafkaProducer.send(new ProducerRecord<>(topic, messageEvent.getData()));

    }

    @Override
    public void onComment(String comment) {
        // do nothing

    }

    @Override
    public void onError(Throwable t) {
        logger.error("Error in stream reading", t);

    }
}
