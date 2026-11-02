package com.nidasehba.bugreport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class TemplateWriterAndAppTest {

    private static List<FailureContext> examples() throws Exception {
        return new ObjectMapper().readValue(Path.of("examples/failures.json").toFile(), new TypeReference<>() {
        });
    }

    @Test
    public void templateReportsForTheExamplesPassValidation() throws Exception {
        TemplateBugWriter writer = new TemplateBugWriter();
        for (FailureContext raw : examples()) {
            FailureContext safe = Redactor.redact(raw);
            BugReport r = writer.write(safe);
            ReportValidator.Result check = ReportValidator.validate(safe, r);
            assertTrue(check.valid(), raw.testName() + ": " + check.problems());
        }
    }

    @Test
    public void severityFollowsTheMoneyImpact() throws Exception {
        List<FailureContext> f = examples();
        TemplateBugWriter w = new TemplateBugWriter();
        assertEquals(w.write(Redactor.redact(f.get(0))).severity(), "BLOCKER");  // refund balance wrong
        assertEquals(w.write(Redactor.redact(f.get(1))).severity(), "CRITICAL"); // 502 on payment
        assertEquals(w.write(Redactor.redact(f.get(2))).severity(), "MAJOR");    // formatting
    }

    @Test
    public void extractsExpectedAndActualValues() throws Exception {
        BugReport r = new TemplateBugWriter().write(Redactor.redact(examples().get(0)));
        assertEquals(r.expectedResult(), "6000");
        assertEquals(r.actualResult(), "10000");
        assertEquals(r.component(), "refund");
    }

    @Test
    public void appWritesMarkdownAndJiraCsvWithoutLeakingSecrets() throws Exception {
        Path out = Files.createTempDirectory("bugs");
        String log = App.run(examples(), new TemplateBugWriter(), out);
        assertTrue(log.startsWith("Writer: template"));
        assertTrue(Files.exists(out.resolve("bug-1.md")));
        String csv = Files.readString(out.resolve("jira-import.csv"));
        assertTrue(csv.startsWith("Summary,Issue Type,Priority,Component/s,Description"));
        assertTrue(csv.contains("\"Highest\""));
        String everything = csv + Files.readString(out.resolve("bug-2.md"));
        assertFalse(everything.contains("4111 1111 1111 1111"));
        assertFalse(everything.contains("sk_test_51HxAbCdEfGhIjKl"));
        assertFalse(everything.contains("jane.doe@example.com"));
    }

    @Test
    public void invalidReportsAreFlaggedForAHumanAndKeptOutOfTheCsv() throws Exception {
        BugWriter liar = new BugWriter() {
            @Override
            public BugReport write(FailureContext f) {
                return new BugReport("Something broke", "x", List.of("run it"), "ok", "not ok", "MAJOR", "x",
                        List.of("this text is not in the failure"));
            }

            @Override
            public String name() {
                return "fake";
            }
        };
        Path out = Files.createTempDirectory("bugs");
        String log = App.run(examples().subList(0, 1), liar, out);
        assertTrue(log.contains("NEEDS_HUMAN"));
        assertTrue(Files.exists(out.resolve("NEEDS_HUMAN-bug-1.md")));
        assertEquals(Files.readString(out.resolve("jira-import.csv")).lines().count(), 1L);
    }
}
