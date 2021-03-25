package ca.uhn.fhir.jpa.starter.documentReference;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import ca.uhn.fhir.jpa.rp.r4.DocumentReferenceResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;

public class RegexDocumentReferenceResourceProvider extends DocumentReferenceResourceProvider {

    @Operation(name = "$regex", idempotent = true)
    public Bundle regexOperation(@OperationParam(name = "pattern") String thePattern,
            @OperationParam(name = "exclude-negations", min = 0, max = 1, typeName = "boolean") IPrimitiveType<Boolean> theExcludeNegations) {
        boolean excludeNegations = theExcludeNegations != null ? theExcludeNegations.getValue() : true;
        return ((IDocumentReferenceDao<DocumentReference>) getDao()).regex(thePattern, excludeNegations);
    }
}
