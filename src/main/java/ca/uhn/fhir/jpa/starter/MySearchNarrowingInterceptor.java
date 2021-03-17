package ca.uhn.fhir.jpa.starter;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.hl7.fhir.r4.model.Encounter;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
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

      // TODO find allowed organizations from Practitioner id and
      // PracitionerRole/Consent
      String[] allowedOrganizations = { authHeader };
      List<String> patientRelatedResources = Arrays.asList("Claim", "DiagnosticReport", "DocumentReference", "Observation");

      if (allowedOrganizations.length == 0) {
         throw new AuthenticationException("Don't have access to any organization");
      }

      IFhirResourceDao<Encounter> encounterResourceProvider = daoRegistry.getResourceDao("Encounter");
      // Filter requests on Patients
      if (theRequestDetails.getResourceName().equals("Patient")) {
         AuthorizedList authList = new AuthorizedList();

         for (String organization : allowedOrganizations) {
            IBundleProvider encountersForAllowedOrganizations = encounterResourceProvider.search(
                  new SearchParameterMap().add(Encounter.SP_SERVICE_PROVIDER, new ReferenceParam(organization)));
            encountersForAllowedOrganizations.getResources(0, encountersForAllowedOrganizations.size()).stream()
                  .map(Encounter.class::cast).forEach(e -> authList.addResource(e.getSubject().getReference()));
         }
         // TODO check that there are some resources in authList, otherwise, shouldn't
         // see anything
         return authList;
      }

      // Filter requests on Encounters
      else if (theRequestDetails.getResourceName().equals("Encounter")) {
         // NOTE no compartment on service-provider so we do it manually
         theRequestDetails.addParameter(Encounter.SP_SERVICE_PROVIDER, allowedOrganizations);
         return new AuthorizedList();
      }

      // Filter requests on Observations
      else if (patientRelatedResources.contains(theRequestDetails.getResourceName())) {
         AuthorizedList authList = new AuthorizedList();

         for (String organization : allowedOrganizations) {
            IBundleProvider encountersForAllowedOrganizations = encounterResourceProvider.search(
                  new SearchParameterMap().add(Encounter.SP_SERVICE_PROVIDER, new ReferenceParam(organization)));
            encountersForAllowedOrganizations.getResources(0, encountersForAllowedOrganizations.size()).stream()
                  .map(Encounter.class::cast).forEach(e -> authList.addCompartment(e.getSubject().getReference()));
         }
         // TODO check that there are some resources in authList, otherwise, shouldn't
         // see anything
         return authList;
      }

      return new AuthorizedList();
   }
}