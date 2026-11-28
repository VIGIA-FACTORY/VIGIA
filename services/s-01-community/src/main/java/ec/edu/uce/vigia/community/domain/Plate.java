package ec.edu.uce.vigia.community.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/** An Ecuadorian plate, three letters and three or four digits, stored as {@code PBA-1234} (D-48). */
public record Plate(String value) {

    private static final Pattern FORMAT = Pattern.compile("([A-Z]{3})-?(\\d{3,4})");

    public static Plate of(String text) {
        String compact =
                text == null ? "" : text.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        var matcher = FORMAT.matcher(compact);
        if (!matcher.matches()) {
            throw new DomainException(ErrorCode.INVALID_PLATE, "The plate must look like PBA-1234");
        }
        return new Plate(matcher.group(1) + "-" + matcher.group(2));
    }
}
