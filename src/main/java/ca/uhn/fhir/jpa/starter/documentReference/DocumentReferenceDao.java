package ca.uhn.fhir.jpa.starter.documentReference;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

    private String makeOptionalRegexPattern(String thePattern) {
        return String.format("(?:%s)?", thePattern);
    }

    private String buildExcludeNegationsRegex(String thePattern) {
        String[] stopWords = { "le", "la", "les", "des", "du" };
        String stopWord = String.format("(?:%s) ", String.join("|", stopWords));
        // FIXME: are you sure ?
        // String word = "[\\w\\-àâçèéêëîïòôöûü]+";
        String keyword = String.format("(%s)[ .,]", thePattern);
        String[] absence = { "pas de signe", "pas", "non", "sans", "n'\\w+ plus", "absence" };
        String space = "[ -]";
        String of = "(?:(?:de )|(?:d'))?";
        // String absenceOf = [prefix.strip() + space + of for prefix in absence]
        String[] absenceOf = Arrays.stream(absence).map(prefix -> prefix + space + of).toArray(String[]::new);
        // absence_of_groups = [f"(?:{prefix})" for prefix in absence_of]
        String[] absenceOfGroups = Arrays.stream(absenceOf).map(prefix -> String.format("(?:%s)", prefix))
                .toArray(String[]::new);
        // absence_regex = f"(?:{'|'.join(absence_of_groups)})"
        String absenceRegex = String.format("(?:%s) ", String.join("|", absenceOfGroups));
        String regex = absenceRegex + makeOptionalRegexPattern(stopWord) + keyword;
        return regex;
    }

    @Transactional
    public Bundle regex(String thePattern, Boolean theExcludeNegations) {
        Bundle bundle = new Bundle();
        SearchSession searchSession = Search.session(myEntityManager);

        SearchPredicateFactory spf = searchSession.scope(ResourceTable.class).predicate();

        String pattern = theExcludeNegations
                ? String.format("(?=%s)(?=!%s)", thePattern, buildExcludeNegationsRegex(thePattern))
                : thePattern;

        PredicateFinalStep finishedQuery = spf.bool(b -> {
            // TODO field where we'll put the contents in the fhir doc
            String contentField = "myContentText";
            String regexpQuery = "{'regexp':{'" + contentField + "':{'value':'" + pattern + "'}}}";
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
