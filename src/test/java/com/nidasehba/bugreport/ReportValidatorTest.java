package com.nidasehba.bugreport;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ReportValidatorTest {

    private static final FailureContext INPUT = new FailureContext("com.demo.api.RefundApiTest#partialRefund",
            "expected [6000] but found [10000]", "java.lang.AssertionError: expected [6000] but found [10000]",
            "GET /v1/payments/pay_8812 -> 200 {\"refundable\":10000}", "staging");

    private static BugReport report(String title, String severity, List<String> evidence, String actual) {
        return new BugReport(title, "Refund did not reduce the refundable amount.", List.of("Create a payment", "Refund 4000"),
                "Refundable 6000", actual, severity, "refunds-service", evidence);
    }

    @Test
    public void acceptsAReportGroundedInTheFailure() {
        BugReport r = report("Partial refund does not reduce refundable balance", "BLOCKER",
                List.of("expected [6000] but found [10000]", "{\"refundable\":10000}"), "Refundable 10000");
        assertTrue(ReportValidator.validate(INPUT, r).valid(), ReportValidator.validate(INPUT, r).problems().toString());
    }

    @Test
    public void rejectsInventedEvidence() {
        BugReport r = report("Partial refund does not reduce refundable balance", "BLOCKER",
                List.of("NullPointerException in RefundService.apply"), "Refundable 10000");
        assertFalse(ReportValidator.validate(INPUT, r).valid());
    }

    @Test
    public void rejectsBadSeverityAndTestNameAsTitle() {
        BugReport r = report(INPUT.testName(), "URGENT", List.of("expected [6000] but found [10000]"), "x");
        List<String> problems = ReportValidator.validate(INPUT, r).problems();
        assertTrue(problems.stream().anyMatch(p -> p.startsWith("invalid severity")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("test name")));
    }

    @Test
    public void rejectsLeakedCardNumbersAndMadeUpLinks() {
        BugReport r = report("Refund wrong for card 4111 1111 1111 1111", "MAJOR",
                List.of("expected [6000] but found [10000]"), "See https://jira.example.com/browse/PAY-1");
        List<String> problems = ReportValidator.validate(INPUT, r).problems();
        assertTrue(problems.contains("report contains a full card number"));
        assertTrue(problems.stream().anyMatch(p -> p.startsWith("link not present")));
    }
}
