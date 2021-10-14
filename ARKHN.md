# JPALTIME

`jpalitme` is Akhn's version of HAPI FHIR. It is a fork of [HAPI FHIR starter project](https://github.com/hapifhir/hapi-fhir-jpaserver-starter).

## Authentication

In HAPI, authentication is achieved through [interceptors](https://hapifhir.io/hapi-fhir/docs/security/authorization_interceptor.html). We do not implement any at the moment. We delegate the responsibility to authenticate users to nginx, using basic authentication. When external authentication is used (like nginx), the [HAPI UI tester](https://hapifhir.io/hapi-fhir/docs/server_plain/web_testpage_overlay.html) needs to be configured to authenticate its requests, this is implemented in `src/main/java/ca/uhn/fhir/jpa/starter/FhirTesterConfig.java`.

## Authorization

We had to implement permissions for the RESAH project. In HAPI, this can be done using [the search narrowing interceptor](https://hapifhir.io/hapi-fhir/docs/security/search_narrowing_interceptor.html). We defined our own (see `src/main/java/ca/uhn/fhir/jpa/starter/interceptors/MySearchNarrowingInterceptor.java`) which can be enabled using the `use_narrowing_interceptor` application property.
