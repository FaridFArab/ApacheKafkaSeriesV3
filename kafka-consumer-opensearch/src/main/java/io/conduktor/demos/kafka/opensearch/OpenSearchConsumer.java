package io.conduktor.demos.kafka.opensearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OpenSearchConsumer {

    public static final Logger logger = LoggerFactory.getLogger(OpenSearchConsumer.class);

    public static RestHighLevelClient createOpenSearchClient(){
        // build a URI from the connection string
        String connString = "http://localhost:9200";
        RestHighLevelClient restHighLevelClient;
        URI connUri = URI.create(connString);

        // extract login information if it exists
        String userInfo = connUri.getUserInfo();

        if (userInfo == null){
            // REST client without security
            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort())));
        }
        else {
            String[] auth = userInfo.split(":");

            CredentialsProvider cp = new BasicCredentialsProvider();
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(auth[0], auth[1]));

            restHighLevelClient = new RestHighLevelClient(RestClient.builder(new HttpHost(connUri.getHost(), connUri.getPort(), connUri.getScheme()))
                    .setHttpClientConfigCallback(HttpAsyncClientBuilder -> HttpAsyncClientBuilder
                            .setDefaultCredentialsProvider(cp).setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())));
        }


        return restHighLevelClient;
    }
    public static void main(String[] args) throws IOException
    {
        // create an OpenSearch Client
        RestHighLevelClient openSearchClient = createOpenSearchClient();

        // create consumer configs
        KafkaConsumer<String, String > kafkaConsumer = createKafkaConsumer();

        // we need to create the index on OpenSearch if it doesn't exist already
        try (openSearchClient; kafkaConsumer)
        {
            if(!openSearchClient.indices().exists(new GetIndexRequest("wikimedia"), RequestOptions.DEFAULT))
            {
            CreateIndexRequest createIndexRequest = new CreateIndexRequest("wikimedia");
            openSearchClient.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            logger.info("The Wikimedia index has been created.");
            }
            else {
                logger.info("The Wikimedia Index already exists!");
            }

            // subscribe consumer to specific topic
            kafkaConsumer.subscribe(Collections.singleton("wikimedia.recentchange"));

            while (true){
                ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(3000));
                logger.info("Received " + records.count() + " record(s)");

                for(ConsumerRecord<String, String> record: records){
                    // send record into OpenSearch
                    try {
                        IndexRequest indexRequest = new IndexRequest("wikimedia").source(record.value(), XContentType.JSON);
                        IndexResponse indexResponse =  openSearchClient.index(indexRequest, RequestOptions.DEFAULT);

                        logger.info(indexResponse.getId());
                    }
                    catch (Exception e){

                    }

                }
            }

//        openSearchClient.close();
        }
    }

    private static KafkaConsumer<String, String> createKafkaConsumer()
    {
        logger.info("I'm a Kafka Consumer");

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"consumer-open-search");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"latest");

        return new KafkaConsumer<>(properties);
    }
}
