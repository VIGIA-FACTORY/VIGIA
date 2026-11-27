package ec.edu.uce.vigia.security;

/** Names of the VIGIA claims in the access token issued by Keycloak (ADR-050, 03.08.04.01 §2). */
public final class VigiaClaims {

    /** List of VIGIA roles of the user, for example {@code ["OWNER", "DRIVER"]}. */
    public static final String ROLE = "vigia:role";

    /** Gatehouse assigned to a guard, absent for other roles. */
    public static final String GATEHOUSE = "vigia:gatehouse";

    /** Prefix that Spring Security expects in front of a role authority. */
    public static final String AUTHORITY_PREFIX = "ROLE_";

    private VigiaClaims() {}
}
