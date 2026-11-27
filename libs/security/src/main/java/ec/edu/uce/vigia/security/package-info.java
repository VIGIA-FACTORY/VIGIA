/**
 * Authorization support shared by the services (05.01 §2.1, 03.08.04.01): the converter that maps the
 * Keycloak claims {@code vigia:role} and {@code vigia:gatehouse} to Spring authorities (ADR-050) and the
 * client of the role-operation matrix owned by S-09. No service imports a Keycloak library. Content is
 * added by the identity stories (US-021, US-022).
 */
package ec.edu.uce.vigia.security;
