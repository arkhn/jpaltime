package ca.uhn.fhir.jpa.starter;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.springframework.beans.factory.annotation.Autowired;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;
import ca.uhn.fhir.jpa.rp.r4.DocumentReferenceResourceProvider;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class RegexDocumentReferenceResourceProvider extends DocumentReferenceResourceProvider {

    @PersistenceContext(type = PersistenceContextType.TRANSACTION)
    private EntityManager myEntityManager;

    // ESClient esClient = new ESClient();

    @Operation(name = "$regex", idempotent = true)
    public Bundle patientTypeOperation(@OperationParam(name = "regex") String theRegex) {
        Bundle bundle = new Bundle();

        // SearchRequest searchRequest = new SearchRequest("document-references");
        // SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // searchSourceBuilder.query(QueryBuilders.regexpQuery("meta.content",
        // theRegex));
        // searchRequest.source(searchSourceBuilder);

        // FhirContext ctx = FhirContext.forR4();
        // IParser parser = ctx.newJsonParser();

        // try {
        // SearchResponse searchResponse =
        // esClient.highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        // for (SearchHit hit : searchResponse.getHits().getHits()) {
        // DocumentReference parsed = parser.parseResource(DocumentReference.class,
        // hit.getSourceAsString());
        // bundle.addEntry().setResource(parsed);
        // }
        // } catch (IOException e) {
        // }

        // TODO inject EntityManager?
        // Configuration config = new Configuration();
        // config.addClass(DocumentReference.class);
        // SessionFactory sessionFactory = config.buildSessionFactory();
        // Session searchSession = SessionFactory.openSession();
        SearchSession searchSession = Search.session(myEntityManager);

        SearchPredicateFactory spf = searchSession.scope(ResourceTable.class).predicate();

        PredicateFinalStep finishedQuery = spf.bool(b -> {
            // TODO field where we'll put the contents in the fhir doc
            String contentField = "meta.content";
            String regexpQuery = "{'regexp':{'" + contentField + "':{'value':'" + theRegex + "'}}}";
            System.out.println("Build Elasticsearch Regexp Query:" + regexpQuery);
            b.must(spf.extension(ElasticsearchExtension.get()).fromJson(regexpQuery));
        });

        SearchQuery<ResourceTable> documentReferencesQuery = searchSession.search(ResourceTable.class)
                .where(f -> finishedQuery).toQuery();

        System.out.println("About to query:" + documentReferencesQuery.queryString());

        List<ResourceTable> documentReferences = documentReferencesQuery.fetchHits(100);
        for (ResourceTable documentReference : documentReferences) {
            // bundle.addEntry().setResource(documentReference.getRes);
            System.out.println(documentReference);
        }

        return bundle;
    }
}
