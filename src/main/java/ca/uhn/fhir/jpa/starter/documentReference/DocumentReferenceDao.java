package ca.uhn.fhir.jpa.starter.documentReference;

import java.util.List;

import javax.transaction.Transactional;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.jpa.dao.BaseHapiFhirResourceDao;
import ca.uhn.fhir.jpa.model.entity.ResourceTable;

public class DocumentReferenceDao extends BaseHapiFhirResourceDao<DocumentReference>
        implements IDocumentReferenceDao<DocumentReference> {

    private static final Logger logger = LoggerFactory.getLogger(DocumentReferenceDao.class);

    @Transactional
    public Bundle regex(String theRegex) {
        Bundle bundle = new Bundle();
        SearchSession searchSession = Search.session(myEntityManager);

        SearchPredicateFactory spf = searchSession.scope(ResourceTable.class).predicate();

        PredicateFinalStep finishedQuery = spf.bool(b -> {
            // TODO field where we'll put the contents in the fhir doc
            String contentField = "myContentText";
            String regexpQuery = "{'regexp':{'" + contentField + "':{'value':'" + theRegex + "'}}}";
            b.must(spf.extension(ElasticsearchExtension.get()).fromJson(regexpQuery));
        });

        SearchQuery<ResourceTable> documentReferencesQuery = searchSession.search(ResourceTable.class)
                .where(f -> finishedQuery).toQuery();

        logger.debug("About to query:" + documentReferencesQuery.queryString());

        // TODO: paginate results
        List<ResourceTable> documentReferences = documentReferencesQuery.fetchHits(100);
        for (ResourceTable documentReference : documentReferences) {
            bundle.addEntry().setResource((DocumentReference) toResource(documentReference, false));
        }

        return bundle;
    }
}
