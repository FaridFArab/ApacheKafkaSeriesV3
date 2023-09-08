package io.conduktor.demos.kafka.opensearch;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

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

        // we need to create the index on OpenSearch if it doesn't exist already
        try (openSearchClient)
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

//        openSearchClient.close();
        }
    }
}
