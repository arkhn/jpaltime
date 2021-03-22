package ca.uhn.fhir.jpa.starter.documentReference;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

public interface IDocumentReferenceDao<T extends IBaseResource> extends IFhirResourceDao<T> {
	Bundle regex(String thePattern, Boolean theExcludeNegations);
}
