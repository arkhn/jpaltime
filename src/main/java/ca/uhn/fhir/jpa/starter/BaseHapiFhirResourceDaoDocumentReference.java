package ca.uhn.fhir.jpa.starter;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.Transactional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import ca.uhn.fhir.jpa.dao.BaseHapiFhirResourceDao;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;

public class BaseHapiFhirResourceDaoDocumentReference extends BaseHapiFhirResourceDao<DocumentReference>
        implements IFhirResourceDaoDocumentReference<DocumentReference> {

    @Transactional
    public Bundle regex(String theRegex) {
        Bundle bundle = new Bundle();
        SearchSession searchSession = Search.session(myEntityManager);

        SearchPredicateFactory spf = searchSession.scope(ResourceTable.class).predicate();

        PredicateFinalStep finishedQuery = spf.bool(b -> {
            // TODO field where we'll put the contents in the fhir doc
            String contentField = "myContentText";
            String regexpQuery = "{'regexp':{'" + contentField + "':{'value':'" + theRegex + "'}}}";
            System.out.println("Build Elasticsearch Regexp Query:" + regexpQuery);
            b.must(spf.extension(ElasticsearchExtension.get()).fromJson(regexpQuery));
        });

        SearchQuery<ResourceTable> documentReferencesQuery = searchSession.search(ResourceTable.class)
                .where(f -> finishedQuery).toQuery();

        System.out.println("About to query:" + documentReferencesQuery.queryString());

        // TODO: paginate results
        List<ResourceTable> documentReferences = documentReferencesQuery.fetchHits(100);
        for (ResourceTable documentReference : documentReferences) {
            bundle.addEntry().setResource((DocumentReference) toResource(documentReference, false));
            System.out.println(documentReference);
        }

        return bundle;
    }
}
