package ca.uhn.fhir.jpa.starter.documentReference;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import ca.uhn.fhir.jpa.rp.r4.DocumentReferenceResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class RegexDocumentReferenceResourceProvider extends DocumentReferenceResourceProvider {

    @Operation(name = "$regex", idempotent = true)
    public Bundle patientTypeOperation(@OperationParam(name = "regex") String theRegex) {
        return ((IDocumentReferenceDao<DocumentReference>) getDao()).regex(theRegex);
    }
}
