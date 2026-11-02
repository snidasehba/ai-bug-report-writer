package com.nidasehba.bugreport;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks a generated report before anyone files it. AI output is treated like any other untrusted input:
 * a report that quotes evidence not present in the failure, leaks a card number or invents a link is
 * rejected and flagged for a human instead.
 */
public final class ReportValidator {

    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private ReportValidator() {
    }

    public record Result(boolean valid, List<String> problems) {
    }

    public static Result validate(FailureContext redactedInput, BugReport r) {
        List<String> problems = new ArrayList<>();
        if (r.title() == null || r.title().isBlank() || r.title().length() > 100) {
            problems.add("title missing or longer than 100 characters");
        } else if (r.title().equals(redactedInput.testName())) {
            problems.add("title is just the test name");
        }
        if (!BugReport.SEVERITIES.contains(r.severity())) {
            problems.add("invalid severity: " + r.severity());
        }
        if (r.stepsToReproduce() == null || r.stepsToReproduce().isEmpty()) {
            problems.add("no steps to reproduce");
        }
        String source = normalise(redactedInput.evidenceText());
        if (r.evidence() == null || r.evidence().isEmpty()) {
            problems.add("no evidence quoted");
        } else {
            for (String e : r.evidence()) {
                if (!source.contains(normalise(e))) {
                    problems.add("evidence not found in the failure: \"" + e + "\"");
                }
            }
        }
        String all = String.join("\n", r.title(), r.summary(), String.join("\n", r.stepsToReproduce()),
                r.expectedResult(), r.actualResult(), String.join("\n", r.evidence()));
        if (Redactor.containsCardNumber(all)) {
            problems.add("report contains a full card number");
        }
        String inputText = redactedInput.evidenceText() + redactedInput.environment();
        URL.matcher(all).results().map(m -> m.group())
                .filter(u -> !inputText.contains(u))
                .forEach(u -> problems.add("link not present in the input: " + u));
        return new Result(problems.isEmpty(), problems);
    }

    private static String normalise(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }
}
