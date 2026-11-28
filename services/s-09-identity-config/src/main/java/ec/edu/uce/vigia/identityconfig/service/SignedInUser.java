package ec.edu.uce.vigia.identityconfig.service;

import java.time.Instant;
import java.util.List;

/** The user of the current request, as S-09 knows it (US-021). */
public record SignedInUser(
        String subject,
        String username,
        String email,
        String displayName,
        List<String> roles,
        String gatehouse,
        Instant firstSeenAt) {}
