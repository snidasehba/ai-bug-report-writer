package com.nidasehba.bugreport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertTrue;

/** Calls Claude on the example failures. Skipped unless ANTHROPIC_API_KEY is set. */
public class LiveBugWriterTest {

    @Test(groups = "llm")
    public void claudeReportsAreGroundedAndSafe() throws Exception {
        if (System.getenv().getOrDefault("ANTHROPIC_API_KEY", "").isBlank()) {
            throw new SkipException("ANTHROPIC_API_KEY not set");
        }
        List<FailureContext> failures = new ObjectMapper().readValue(Path.of("examples/failures.json").toFile(),
                new TypeReference<>() {
                });
        ClaudeBugWriter writer = ClaudeBugWriter.fromEnv();
        int valid = 0;
        for (FailureContext raw : failures) {
            FailureContext safe = Redactor.redact(raw);
            ReportValidator.Result r = ReportValidator.validate(safe, writer.write(safe));
            assertTrue(r.problems().stream().noneMatch(p -> p.contains("card number")), r.problems().toString());
            if (r.valid()) {
                valid++;
            }
        }
        assertTrue(valid >= failures.size() - 1, "only " + valid + " valid reports");
    }
}
