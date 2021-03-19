package ca.uhn.fhir.jpa.starter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Consent.ConsentProvisionType;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;

public class MyConsentService implements IConsentService {

   private final IFhirResourceDao<Consent> myConsentDao;

   MyConsentService(DaoRegistry daoRegistry) {
      myConsentDao = daoRegistry.getResourceDao("Consent");
   }

   @Override
   public ConsentOutcome startOperation(RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
      // This means that all requests should flow through the consent service
      // This has performance implications - If you know that some requests
      // don't need consent checking it is a good idea to return
      // ConsentOutcome.AUTHORIZED instead for those requests.
      return ConsentOutcome.PROCEED;
   }

   @Override
   public ConsentOutcome canSeeResource(RequestDetails theRequestDetails, IBaseResource theResource,
         IConsentContextServices theContextServices) {

      // Find the reference for the Patient associated to theResource
      String patRef = "";
      if (theResource instanceof Patient) { // Patient
         Patient pat = (Patient) theResource;
         patRef = pat.getIdElement().toVersionless().getValue();
      } else {
         try {
            Reference pat = (Reference) theResource.getClass().getMethod("getSubject").invoke(theResource);
            if (pat.getType().equals("Patient"))
               patRef = pat.getReference();
         } catch (Exception e) {
         }
      }

      if (patRef.equals("")) {
         return ConsentOutcome.AUTHORIZED;
      }

      // FIXME Could you avoid the newSynchronous?
      SearchParameterMap consentSearchParams = SearchParameterMap.newSynchronous().add(Consent.SP_PATIENT,
            new ReferenceParam(patRef));
      IBundleProvider consentsForPatient = myConsentDao.search(consentSearchParams);
      // Look for any deny consent
      if (consentsForPatient.getResources(0, consentsForPatient.size()).stream().map(Consent.class::cast)
            .anyMatch(c -> c.getProvision().getType() == ConsentProvisionType.DENY)) {
         return ConsentOutcome.REJECT;
      }

      return ConsentOutcome.AUTHORIZED;
   }
}

interface IPatientRelatedResource {
   public Reference getSubject();
}