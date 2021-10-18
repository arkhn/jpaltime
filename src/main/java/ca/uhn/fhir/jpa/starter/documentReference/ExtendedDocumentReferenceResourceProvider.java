package ca.uhn.fhir.jpa.starter.documentReference;

import java.io.IOException;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.DocumentReference.DocumentReferenceContentComponent;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.jpa.rp.r4.DocumentReferenceResourceProvider;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.StringParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Overrides the CRUD operations to use the custom index. Implements searching
 * through that index.
 */
public class ExtendedDocumentReferenceResourceProvider extends DocumentReferenceResourceProvider {

    private static final String INDEX_NAME = "document-reference-content";
    private static final String ID_FIELD_NAME = "unqualified_versionless_id";
    private static final String CONTENT_QUERY_PARAM = "content";
    private static final String CONTENT_FIELD_NAME = "content";

    private static final Logger LOG = LoggerFactory.getLogger(IndexCreationEventListener.class);

    /**
     * Indexes the document's attachment data. Ignores the version.
     *
     * @param theResource
     * @throws IOException
     */
    protected static void indexDocument(DocumentReference theResource) throws IOException {
        RestHighLevelClient client = ElasticsearchClientBuilder.build();

        for (DocumentReferenceContentComponent content : theResource.getContent()) {
            XContentBuilder builder;
            builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.field(CONTENT_FIELD_NAME, new String(content.getAttachment().getData()));
                builder.field(ID_FIELD_NAME, theResource.getIdElement().toUnqualifiedVersionless().getValue());
            }
            builder.endObject();
            IndexRequest request = new IndexRequest(INDEX_NAME).source(builder);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);

            LOG.info(response.toString());
        }

    }

    /**
     * Searches through the indexed data.
     *
     * @param searchString
     * @return
     * @throws IOException
     */
    protected static List<String> searchDocuments(String searchString) throws IOException {
        RestHighLevelClient client = ElasticsearchClientBuilder.build();
        List<String> results = new ArrayList<String>();

        // 1. Search the indexed data for matching document. Retrieve a list of Document
        // IDs.
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.termQuery(CONTENT_FIELD_NAME, searchString));

        SearchRequest searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        // 2. Retrieve the list of matching documents by their ID.
        for (SearchHit hit : searchResponse.getHits()) {
            results.add(hit.getSourceAsMap().get(ID_FIELD_NAME).toString());
        }
        return results;
    }

    protected static void deleteDocuments(IIdType theResource) throws IOException {
        RestHighLevelClient client = ElasticsearchClientBuilder.build();

        DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_NAME)
                .setQuery(new TermQueryBuilder(ID_FIELD_NAME, theResource.toUnqualifiedVersionless().getValue()));
        BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);

        LOG.info(response.toString());
    }

    @Create
    public MethodOutcome create(HttpServletRequest theRequest, @ResourceParam DocumentReference theResource,
            @ConditionalUrlParam String theConditional, RequestDetails theRequestDetails) {

        startRequest(theRequest);
        try {
            MethodOutcome retVal;
            if (theConditional != null) {
                retVal = getDao().create(theResource, theConditional, theRequestDetails);
            } else {
                retVal = getDao().create(theResource, theRequestDetails);
            }

            try {
                indexDocument(theResource);
            } catch (IOException exc) {
            }

            return retVal;

        } finally {
            endRequest(theRequest);
        }
    }

    @Search
    public List<DocumentReference> search(@RequiredParam(name = CONTENT_QUERY_PARAM) StringParam searchString) {
        List<DocumentReference> retVal = new ArrayList<DocumentReference>();

        List<String> hits = null;
        try {
            hits = searchDocuments(searchString.getValue());
        } catch (IOException exc) {
        }

        for (String hit : hits) {
            IdType id = new IdType(hit);
            DocumentReference documentReference = getDao().read(id);
            retVal.add(documentReference);
        }

        return retVal;
    }

    @Delete()
    public MethodOutcome delete(HttpServletRequest theRequest, @IdParam IIdType theResource,
            @ConditionalUrlParam(supportsMultiple = true) String theConditional, RequestDetails theRequestDetails) {
        startRequest(theRequest);
        try {
            MethodOutcome retVal;
            if (theConditional != null) {
                retVal = getDao().deleteByUrl(theConditional, theRequestDetails);
            } else {
                retVal = getDao().delete(theResource, theRequestDetails);
            }

            try {
                deleteDocuments(theResource);
            } catch (IOException exc) {
            }

            return retVal;
        } finally {
            endRequest(theRequest);
        }
    }

}
