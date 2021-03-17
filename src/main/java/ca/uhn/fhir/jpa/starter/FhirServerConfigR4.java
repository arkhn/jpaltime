package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.search.lastn.ElasticsearchSvcImpl;
import ca.uhn.fhir.jpa.starter.annotations.OnR4Condition;
import ca.uhn.fhir.jpa.starter.cql.StarterCqlR4Config;
import ca.uhn.fhir.jpa.starter.documentReference.DocumentReferenceDao;
import ca.uhn.fhir.jpa.starter.documentReference.RegexDocumentReferenceResourceProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@Conditional(OnR4Condition.class)
@Import(StarterCqlR4Config.class)
public class FhirServerConfigR4 extends BaseJavaConfigR4 {

  @Autowired
  private DataSource myDataSource;

  /**
   * We override the paging provider definition so that we can customize the
   * default/max page sizes for search results. You can set these however you
   * want, although very large page sizes will require a lot of RAM.
   */
  @Autowired
  AppProperties appProperties;

  @Override
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
    pagingProvider.setDefaultPageSize(appProperties.getDefault_page_size());
    pagingProvider.setMaximumPageSize(appProperties.getMax_page_size());
    return pagingProvider;
  }

  @Autowired
  private ConfigurableEnvironment configurableEnvironment;

  @Override
  @Bean()
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
    retVal.setPersistenceUnitName("HAPI_PU");

    try {
      retVal.setDataSource(myDataSource);
    } catch (Exception e) {
      throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
    }

    retVal.setJpaProperties(EnvironmentHelper.getHibernateProperties(configurableEnvironment));
    return retVal;
  }

  @Bean
  @Primary
  public JpaTransactionManager hapiTransactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  @Bean()
  public ElasticsearchSvcImpl elasticsearchSvc() {
    if (EnvironmentHelper.isElasticsearchEnabled(configurableEnvironment)) {
      String elasticsearchUrl = EnvironmentHelper.getElasticsearchServerUrl(configurableEnvironment);
      String elasticsearchHost;
      if (elasticsearchUrl.startsWith("http")) {
        elasticsearchHost = elasticsearchUrl.substring(elasticsearchUrl.indexOf("://") + 3,
            elasticsearchUrl.lastIndexOf(":"));
      } else {
        elasticsearchHost = elasticsearchUrl.substring(0, elasticsearchUrl.indexOf(":"));
      }

      String elasticsearchUsername = EnvironmentHelper.getElasticsearchServerUsername(configurableEnvironment);
      String elasticsearchPassword = EnvironmentHelper.getElasticsearchServerPassword(configurableEnvironment);
      int elasticsearchPort = Integer.parseInt(elasticsearchUrl.substring(elasticsearchUrl.lastIndexOf(":") + 1));
      return new ElasticsearchSvcImpl(elasticsearchHost, elasticsearchPort, elasticsearchUsername,
          elasticsearchPassword);
    } else {
      return null;
    }
  }

  @Override
  @Bean(name = "myDocumentReferenceDaoR4", autowire = Autowire.BY_NAME)
  public IFhirResourceDao<org.hl7.fhir.r4.model.DocumentReference> daoDocumentReferenceR4() {

    DocumentReferenceDao retVal;
    retVal = new DocumentReferenceDao();
    retVal.setResourceType(org.hl7.fhir.r4.model.DocumentReference.class);
    retVal.setContext(fhirContextR4());
    return retVal;
  }

  @Override
  @Bean(name = "myDocumentReferenceRpR4")
  @Lazy
  public ca.uhn.fhir.jpa.rp.r4.DocumentReferenceResourceProvider rpDocumentReferenceR4() {
    RegexDocumentReferenceResourceProvider retVal;
    retVal = new RegexDocumentReferenceResourceProvider();
    retVal.setContext(fhirContextR4());
    retVal.setDao(daoDocumentReferenceR4());
    return retVal;
  }

}
