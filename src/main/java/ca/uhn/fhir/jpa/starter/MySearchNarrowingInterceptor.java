package ca.uhn.fhir.jpa.starter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.PractitionerRole;

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

   IFhirResourceDao<Encounter> encounterResourceProvider;
   IFhirResourceDao<PractitionerRole> practitionerRoleResourceProvider;

   MySearchNarrowingInterceptor(DaoRegistry daoRegistry) {
      encounterResourceProvider = daoRegistry.getResourceDao("Encounter");
      practitionerRoleResourceProvider = daoRegistry.getResourceDao("PractitionerRole");
   }

   @Override
   protected AuthorizedList buildAuthorizedList(RequestDetails theRequestDetails) {
      // In this basic example we have two hardcoded bearer tokens,
      // one which is for a user that has access to one patient, and
      // another that has full access.
      String authHeader = theRequestDetails.getHeader("Authorization");
      if (authHeader.isEmpty()) {
         throw new AuthenticationException("Missing authorization token");
      } else if (authHeader.equals("Bearer adminToken")) {
         // This user has access to everything
         return new AuthorizedList();
      }
      String practitionerId = String.format("Practitioner/%s", authHeader);

      // Find Organizations for Pracatitioner
      IBundleProvider rolesForPractitioner = practitionerRoleResourceProvider
            .search(new SearchParameterMap().add(PractitionerRole.SP_PRACTITIONER, new ReferenceParam(practitionerId)));
      List<String> allowedOrganizations = rolesForPractitioner.getResources(0, rolesForPractitioner.size()).stream()
            .map(PractitionerRole.class::cast).map(p -> p.getOrganization().getReference())
            .collect(Collectors.toList());

      if (allowedOrganizations.isEmpty()) {
         throw new AuthenticationException("Don't have access to any organization");
      }

      // Find patients the user is allowed to see
      List<String> allowedPatientRefs = new ArrayList();
      for (String organization : allowedOrganizations) {
         IBundleProvider encountersForAllowedOrganizations = encounterResourceProvider
               .search(new SearchParameterMap().add(Encounter.SP_SERVICE_PROVIDER, new ReferenceParam(organization)));
         encountersForAllowedOrganizations.getResources(0, encountersForAllowedOrganizations.size()).stream()
               .map(Encounter.class::cast).forEach(e -> allowedPatientRefs.add(e.getSubject().getReference()));
      }

      if (allowedPatientRefs.isEmpty()) {
         throw new AuthenticationException("Don't have access to any patient");
      }

      List<String> patientRelatedResources = Arrays.asList("Claim", "DiagnosticReport", "DocumentReference",
            "Encounter", "Observation", "Procedure");

      // Filter requests on Patients
      if (theRequestDetails.getResourceName().equals("Patient")) {
         AuthorizedList authList = new AuthorizedList();
         allowedPatientRefs.forEach(authList::addResources);
         return authList;
      }
      // Filter requests on other resources
      else if (patientRelatedResources.contains(theRequestDetails.getResourceName())) {
         AuthorizedList authList = new AuthorizedList();
         allowedPatientRefs.forEach(authList::addCompartment);
         return authList;
      }

      return new AuthorizedList();
   }
}