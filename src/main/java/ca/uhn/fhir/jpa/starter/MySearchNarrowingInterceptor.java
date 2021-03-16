package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizedList;
import ca.uhn.fhir.rest.server.interceptor.auth.SearchNarrowingInterceptor;

public class MySearchNarrowingInterceptor extends SearchNarrowingInterceptor {

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

      return new AuthorizedList().addCompartment(String.format("Practitioner/%s", authHeader));
   }
}