package com.nidasehba.bugreport;

import java.util.regex.Pattern;

/**
 * Masks secrets and personal data before anything leaves the machine. Test logs from payments systems
 * routinely contain card numbers, bearer tokens, API keys and customer emails; none of them belong in a
 * prompt or a Jira ticket.
 */
public final class Redactor {

    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]?){12,15}(\\d{4})\\b");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/=-]{8,}");
    private static final Pattern API_KEY = Pattern.compile("(?i)((?:api[_-]?key|secret|password|token)\"?\\s*[:=]\\s*\"?)[^\\s\",}]{4,}");
    private static final Pattern EMAIL = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    private Redactor() {
    }

    public static String redact(String text) {
        if (text == null) {
            return "";
        }
        String out = CARD.matcher(text).replaceAll("****$1");
        out = BEARER.matcher(out).replaceAll("$1[REDACTED]");
        out = API_KEY.matcher(out).replaceAll("$1[REDACTED]");
        return EMAIL.matcher(out).replaceAll("[EMAIL]");
    }

    public static FailureContext redact(FailureContext c) {
        return new FailureContext(c.testName(), redact(c.errorMessage()), redact(c.stackTrace()),
                redact(c.logExcerpt()), redact(c.environment()));
    }

    /** True if the text still contains something that looks like a full card number. */
    static boolean containsCardNumber(String text) {
        return text != null && CARD.matcher(text).find();
    }
}
