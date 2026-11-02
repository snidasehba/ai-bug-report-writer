package com.nidasehba.bugreport;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/** A Jira-ready bug; also the structured-output schema sent to the model. */
public record BugReport(
        @JsonPropertyDescription("Specific one-line title, under 100 characters: what is broken and where. Not the test name.")
        String title,
        @JsonPropertyDescription("Two or three sentences a developer can act on")
        String summary,
        @JsonPropertyDescription("Numbered steps a person could follow to reproduce, derived from the test and logs")
        List<String> stepsToReproduce,
        @JsonPropertyDescription("What should have happened")
        String expectedResult,
        @JsonPropertyDescription("What actually happened, with the concrete values from the failure")
        String actualResult,
        @JsonPropertyDescription("One of: BLOCKER, CRITICAL, MAJOR, MINOR")
        String severity,
        @JsonPropertyDescription("The service or screen most likely at fault, e.g. refunds-service")
        String component,
        @JsonPropertyDescription("Exact fragments copied from the failure text that prove the bug")
        List<String> evidence) {

    public static final List<String> SEVERITIES = List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR");
}
