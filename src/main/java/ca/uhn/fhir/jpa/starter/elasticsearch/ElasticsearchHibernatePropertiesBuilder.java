package ca.uhn.fhir.jpa.starter.elasticsearch;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.search.lastn.ElasticsearchRestClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.PutIndexTemplateRequest;
import org.elasticsearch.common.settings.Settings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.mapper.orm.schema.management.SchemaManagementStrategyName;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class is used to inject appropriate properties into a hibernate
 * Properties object being used to create an entitymanager for a HAPI FHIR JPA
 * server. This class also injects a starter template into the ES cluster.
 */
public class ElasticsearchHibernatePropertiesBuilder {
	private static final Logger ourLog = getLogger(ElasticsearchHibernatePropertiesBuilder.class);

	private IndexStatus myRequiredIndexStatus = IndexStatus.YELLOW.YELLOW;
	private SchemaManagementStrategyName myIndexSchemaManagementStrategy = SchemaManagementStrategyName.CREATE;

	private String myRestUrl;
	private String myUsername;
	private String myPassword;
	private long myIndexManagementWaitTimeoutMillis = 10000L;
	private String myDebugSyncStrategy = AutomaticIndexingSynchronizationStrategyNames.ASYNC;
	private boolean myDebugPrettyPrintJsonLog = false;
	private String myProtocol;

	public ElasticsearchHibernatePropertiesBuilder setUsername(String theUsername) {
		myUsername = theUsername;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setPassword(String thePassword) {
		myPassword = thePassword;
		return this;
	}

	public void apply(Properties theProperties) {

		// the below properties are used for ElasticSearch integration
		theProperties.put(BackendSettings.backendKey(BackendSettings.TYPE), "elasticsearch");

		// NOTE this class has been overriden because of this: we want to use our
		// analyzers
		theProperties.put(BackendSettings.backendKey(ElasticsearchIndexSettings.ANALYSIS_CONFIGURER),
				MyElasticsearchAnalysisConfigurer.class.getName());

		theProperties.put(BackendSettings.backendKey(ElasticsearchBackendSettings.HOSTS), myRestUrl);
		theProperties.put(BackendSettings.backendKey(ElasticsearchBackendSettings.PROTOCOL), myProtocol);

		if (StringUtils.isNotBlank(myUsername)) {
			theProperties.put(BackendSettings.backendKey(ElasticsearchBackendSettings.USERNAME), myUsername);
		}
		if (StringUtils.isNotBlank(myPassword)) {
			theProperties.put(BackendSettings.backendKey(ElasticsearchBackendSettings.PASSWORD), myPassword);
		}
		theProperties.put(HibernateOrmMapperSettings.SCHEMA_MANAGEMENT_STRATEGY,
				myIndexSchemaManagementStrategy.externalRepresentation());
		theProperties.put(
				BackendSettings
						.backendKey(ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT),
				Long.toString(myIndexManagementWaitTimeoutMillis));
		theProperties.put(
				BackendSettings.backendKey(ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS),
				myRequiredIndexStatus.externalRepresentation());
		// Need the mapping to be dynamic because of terminology indexes.
		theProperties.put(BackendSettings.backendKey(ElasticsearchIndexSettings.DYNAMIC_MAPPING), "true");
		// Only for unit tests
		theProperties.put(HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY, myDebugSyncStrategy);
		theProperties.put(BackendSettings.backendKey(ElasticsearchBackendSettings.LOG_JSON_PRETTY_PRINTING),
				Boolean.toString(myDebugPrettyPrintJsonLog));

		injectStartupTemplate(myProtocol, myRestUrl, myUsername, myPassword);

	}

	public ElasticsearchHibernatePropertiesBuilder setRequiredIndexStatus(IndexStatus theRequiredIndexStatus) {
		myRequiredIndexStatus = theRequiredIndexStatus;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setRestUrl(String theRestUrl) {
		if (theRestUrl.contains("://")) {
			throw new ConfigurationException(
					"Elasticsearch URL cannot include a protocol, that is a separate property. Remove http:// or https:// from this URL.");
		}
		myRestUrl = theRestUrl;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setProtocol(String theProtocol) {
		myProtocol = theProtocol;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setIndexSchemaManagementStrategy(
			SchemaManagementStrategyName theIndexSchemaManagementStrategy) {
		myIndexSchemaManagementStrategy = theIndexSchemaManagementStrategy;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setIndexManagementWaitTimeoutMillis(
			long theIndexManagementWaitTimeoutMillis) {
		myIndexManagementWaitTimeoutMillis = theIndexManagementWaitTimeoutMillis;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setDebugIndexSyncStrategy(String theSyncStrategy) {
		myDebugSyncStrategy = theSyncStrategy;
		return this;
	}

	public ElasticsearchHibernatePropertiesBuilder setDebugPrettyPrintJsonLog(boolean theDebugPrettyPrintJsonLog) {
		myDebugPrettyPrintJsonLog = theDebugPrettyPrintJsonLog;
		return this;
	}

	void injectStartupTemplate(String theProtocol, String theHostAndPort, String theUsername, String thePassword) {
		PutIndexTemplateRequest ngramTemplate = new PutIndexTemplateRequest("ngram-template")
				.patterns(Arrays.asList("resourcetable-*", "termconcept-*"))
				.settings(Settings.builder().put("index.max_ngram_diff", 50));

		int colonIndex = theHostAndPort.indexOf(":");
		String host = theHostAndPort.substring(0, colonIndex);
		Integer port = Integer.valueOf(theHostAndPort.substring(colonIndex + 1));
		String qualifiedHost = theProtocol + "://" + host;

		try {
			RestHighLevelClient elasticsearchHighLevelRestClient = ElasticsearchRestClientFactory
					.createElasticsearchHighLevelRestClient(qualifiedHost, port, theUsername, thePassword);
			ourLog.info("Adding starter template for large ngram diffs!!!!!!!!!!");
			AcknowledgedResponse acknowledgedResponse = elasticsearchHighLevelRestClient.indices()
					.putTemplate(ngramTemplate, RequestOptions.DEFAULT);
			assert acknowledgedResponse.isAcknowledged();
		} catch (IOException theE) {
			theE.printStackTrace();
			throw new ConfigurationException(
					"Couldn't connect to the elasticsearch server to create necessary templates. Ensure the Elasticsearch user has permissions to create templates.");
		}
	}
}
