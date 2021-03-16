package ca.uhn.fhir.jpa.starter;

import java.util.List;

import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

@SuppressWarnings("ConstantConditions")
public class MyAuthorizationInterceptor extends AuthorizationInterceptor {

   @Override
   public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {

      // Process authorization header - The following is a fake
      // implementation. Obviously we'd want something more real
      // for a production scenario.
      //
      // In this basic example we have two hardcoded bearer tokens,
      // one which is for a user that has access to one patient, and
      // another that has full access.
      String authHeader = theRequestDetails.getHeader("Authorization");
      if ("Bearer adminToken".equals(authHeader)) {
         // This user has access to everything
         // If the user is an admin, allow everything
         return new RuleBuilder().allowAll().build();
      }

      return new RuleBuilder().allow("practitioner-read").read().allResources()
            .inCompartment("Practitioner", new IdType("Practitioner", authHeader)).andThen().denyAll("practitioner-deny").build();
   }
}
