package ca.uhn.fhir.jpa.starter.documentReference;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class IndexCreationEventListener {

    private static final String INDEX_NAME = "document-reference-content";
    private static final Logger LOG = LoggerFactory.getLogger(IndexCreationEventListener.class);

    protected static boolean indexExists(RestHighLevelClient client) throws IOException {
        GetIndexRequest request = new GetIndexRequest(INDEX_NAME);
        return client.indices().exists(request, RequestOptions.DEFAULT);
    }

    protected static void createIndex(RestHighLevelClient client) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(INDEX_NAME);
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();
        {
            builder.startObject("properties");
            {
                builder.startObject("content");
                {
                    builder.field("type", "text");
                }
                builder.endObject();
                builder.startObject("unqualified_versionless_id");
                {
                    builder.field("type", "text");
                }
                builder.endObject();
            }
            builder.endObject();
        }
        builder.endObject();
        request.mapping(builder);

        client.indices().create(request, RequestOptions.DEFAULT);
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) throws IOException {
        RestHighLevelClient client = ElasticsearchClientBuilder.build();

        try {
            if (!indexExists(client)) {
                createIndex(client);
            }
        } finally {
            client.close();
        }
    }
}