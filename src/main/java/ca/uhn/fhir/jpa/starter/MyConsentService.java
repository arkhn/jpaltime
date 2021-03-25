package ca.uhn.fhir.jpa.starter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Procedure;
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
         Patient res = (Patient) theResource;
         patRef = res.getIdElement().toVersionless().getValue();
      } else if (theResource instanceof Observation) { // Observation
         Observation res = (Observation) theResource;
         patRef = res.getSubject().getReference();
      } else if (theResource instanceof DocumentReference) { // DocumentReference
         DocumentReference res = (DocumentReference) theResource;
         patRef = res.getSubject().getReference();
      } else if (theResource instanceof DiagnosticReport) { // DiagnosticReport
         DiagnosticReport res = (DiagnosticReport) theResource;
         patRef = res.getSubject().getReference();
      } else if (theResource instanceof Encounter) { // Encounter
         Encounter res = (Encounter) theResource;
         patRef = res.getSubject().getReference();
      } else if (theResource instanceof Procedure) { // Procedure
         Procedure res = (Procedure) theResource;
         patRef = res.getSubject().getReference();
      } else if (theResource instanceof Claim) { // Claim
         Claim res = (Claim) theResource;
         patRef = res.getPatient().getReference();
      } else {
         return ConsentOutcome.AUTHORIZED;
      }

      // FIXME Could you avoid the newSynchronous? Without it, the search query never ends
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
