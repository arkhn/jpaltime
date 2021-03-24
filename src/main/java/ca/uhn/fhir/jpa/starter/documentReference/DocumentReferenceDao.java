package ca.uhn.fhir.jpa.starter.documentReference;

import java.util.Arrays;
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

    private String makeOptionalRegexPattern(String pattern) {
        return String.format("(%s)?", pattern);
    }

    // private String makeNegativeLookbehind(String pattern) {
    // return String.format("(?\\\\<!%s)", pattern);
    // }

    private String buildExcludeNegationsRegex(String thePattern) {
        String stopWords = "(le|la|les|des|du) ";
        // String[] absence = { "pas de signe", "pas", "non", "sans", "absence",
        // "n\\\\'\\w+ plus" };
        String[] absence = { "pas de signe", "pas", "non", "sans", "absence" };
        // String of = "(?:(?:de )|(?:d\\\\'))?";
        String of = makeOptionalRegexPattern("de ");
        String[] absenceOf = Arrays.stream(absence).map(prefix -> prefix + " " + of).toArray(String[]::new);
        String absenceRegex = String.format("(%s)", String.join("|", absenceOf));
        // String regex = makeNegativeLookbehind(absenceRegex +
        // makeOptionalRegexPattern(stopWords)) + thePattern;
        String regex = absenceRegex + makeOptionalRegexPattern(stopWords) + thePattern;
        return regex;
    }

    @Transactional
    public Bundle regex(String thePattern, Boolean theExcludeNegations) {
        Bundle bundle = new Bundle();
        SearchSession searchSession = Search.session(myEntityManager);

        SearchPredicateFactory spf = searchSession.scope(ResourceTable.class).predicate();

        PredicateFinalStep finishedQuery = spf.bool(b -> {
            String contentField = "myContentText";
            String regexpQuery = "{'regexp':{'" + contentField + "':{'value':'.*" + thePattern + ".*'}}}";
            b.must(spf.extension(ElasticsearchExtension.get()).fromJson(regexpQuery));
            String resourceTypeField = "myResourceType";
            String resourceTypeQuery = "{'match':{'" + resourceTypeField + "':{'query': 'DocumentReference' }}}";
            b.must(spf.extension(ElasticsearchExtension.get()).fromJson(resourceTypeQuery));
            if (theExcludeNegations) {
                String excludeNegPattern = buildExcludeNegationsRegex(thePattern);
                String excludeNegRegexpQuery = "{'regexp':{'" + contentField + "':{'value':'.*" + excludeNegPattern + ".*'}}}";
                b.mustNot(spf.extension(ElasticsearchExtension.get()).fromJson(excludeNegRegexpQuery));
            }
        });

        SearchQuery<ResourceTable> documentReferencesQuery = searchSession.search(ResourceTable.class)
                .where(f -> finishedQuery).toQuery();

        logger.info("About to query:" + documentReferencesQuery.queryString());

        // TODO: Do we need to paginate results for performance reasons?
        List<ResourceTable> documentReferences = documentReferencesQuery.fetchAllHits();
        for (ResourceTable documentReference : documentReferences) {
            bundle.addEntry().setResource((DocumentReference) toResource(documentReference, false));
        }

        return bundle;
    }
}
