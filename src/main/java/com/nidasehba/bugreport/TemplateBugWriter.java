package com.nidasehba.bugreport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A deterministic, rule-based writer. It runs without an API key, gives the AI writer a baseline to beat,
 * and keeps the pipeline working if the API is unavailable.
 */
public class TemplateBugWriter implements BugWriter {

    private static final Pattern EXPECTED = Pattern.compile("expected:?\\s*\\[?<?([^>\\]]+)>?\\]?\\s*but (?:was|found):?\\s*\\[?<?([^>\\]]+)>?\\]?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STATUS = Pattern.compile("\\b([45]\\d\\d)\\b");

    @Override
    public BugReport write(FailureContext f) {
        String[] parts = f.testName().split("[#.]");
        String method = parts[parts.length - 1];
        String clazz = parts.length > 1 ? parts[parts.length - 2] : method;
        String readable = humanise(method);
        String component = humanise(clazz.replaceAll("(?i)(Api|Ui)?Test$", "")).toLowerCase(Locale.ROOT).replace(' ', '-');

        String expected = "The test step \"" + readable + "\" passes";
        String actual = f.errorMessage();
        Matcher m = EXPECTED.matcher(f.errorMessage());
        if (m.find()) {
            expected = m.group(1).trim();
            actual = m.group(2).trim();
        }

        List<String> evidence = new ArrayList<>();
        evidence.add(f.errorMessage());
        f.logExcerpt().lines().filter(l -> l.contains("ERROR") || STATUS.matcher(l).find()).limit(2).forEach(evidence::add);

        List<String> steps = List.of(
                "Use environment: " + f.environment(),
                "Run " + f.testName() + " (" + readable + ")",
                "Observe: " + f.errorMessage());
        String title = truncate(capitalise(readable) + " fails: " + f.errorMessage(), 100);
        return new BugReport(title, "Automated test " + f.testName() + " failed in " + f.environment() + ".",
                steps, expected, actual, severityFor(f), component, evidence);
    }

    static String severityFor(FailureContext f) {
        String text = (f.testName() + " " + f.evidenceText()).toLowerCase(Locale.ROOT);
        if (text.contains("double") || text.contains("refund") && text.contains("balance") || text.contains("ledger")) {
            return "BLOCKER";
        }
        Matcher status = STATUS.matcher(f.errorMessage());
        if (status.find() && status.group(1).startsWith("5")) {
            return "CRITICAL";
        }
        return "MAJOR";
    }

    static String humanise(String camel) {
        return camel.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.ROOT).trim();
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    @Override
    public String name() {
        return "template";
    }
}
