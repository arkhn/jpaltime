package ca.uhn.fhir.jpa.starter;

import java.util.List;
import java.util.ArrayList;

import org.hl7.fhir.r4.model.Encounter;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizedList;
import ca.uhn.fhir.rest.server.interceptor.auth.SearchNarrowingInterceptor;

public class MySearchNarrowingInterceptor extends SearchNarrowingInterceptor {

   DaoRegistry daoRegistry;

   MySearchNarrowingInterceptor(DaoRegistry daoRegistry) {
      this.daoRegistry = daoRegistry;
   }

   @Override
   protected AuthorizedList buildAuthorizedList(RequestDetails theRequestDetails) {
      // In this basic example we have two hardcoded bearer tokens,
      // one which is for a user that has access to one patient, and
      // another that has full access.
      String authHeader = theRequestDetails.getHeader("Authorization");
      if ("Bearer adminToken".equals(authHeader)) {
         // This user has access to everything
         return new AuthorizedList();
      }

      // Filter requests on Patients
      if (theRequestDetails.getResourceName().equals("Patient")) {
         AuthorizedList authList = new AuthorizedList();
         IFhirResourceDao<Encounter> encounterResourceProvider = daoRegistry.getResourceDao("Encounter");

         // TODO find allowed organizations from Practitioner id and PracitionerRole/Consent
         List<String> allowedOrganizations = new ArrayList<>();
         allowedOrganizations.add(authHeader);

         allowedOrganizations.forEach(organization -> {
            IBundleProvider encountersForAllowedOrganizations = encounterResourceProvider
                  .search(new SearchParameterMap().add(Encounter.SP_SERVICE_PROVIDER, new ReferenceParam(authHeader)));
            encountersForAllowedOrganizations.getResources(0, encountersForAllowedOrganizations.size()).stream()
                  .map(Encounter.class::cast).forEach(e -> authList.addResource(e.getSubject().getReference()));
         });
         return authList;
      }

      return new AuthorizedList();
   }
}