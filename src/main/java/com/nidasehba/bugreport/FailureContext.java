package com.nidasehba.bugreport;

/**
 * Everything known about one failed test: what a tester would paste into a bug by hand.
 *
 * @param environment e.g. "staging, build 2026.11.02-3, Chrome 131"
 */
public record FailureContext(String testName, String errorMessage, String stackTrace, String logExcerpt,
                             String environment) {

    /** All the text the model is allowed to quote from, after redaction. */
    public String evidenceText() {
        return String.join("\n", errorMessage, stackTrace, logExcerpt);
    }
}
