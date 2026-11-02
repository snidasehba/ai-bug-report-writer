package com.nidasehba.bugreport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Usage: java -jar ai-bug-report-writer.jar <failures.json> [out-dir]
 *
 * <p>Uses Claude when ANTHROPIC_API_KEY is set, the template writer otherwise. Writes one Markdown file per
 * failure plus jira-import.csv. Reports that fail validation are written with a NEEDS_HUMAN prefix and left
 * out of the CSV.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: <failures.json> [out-dir]");
            System.exit(2);
        }
        Path out = Path.of(args.length > 1 ? args[1] : "bug-reports");
        List<FailureContext> failures = new ObjectMapper().readValue(Path.of(args[0]).toFile(), new TypeReference<>() {
        });
        BugWriter writer = System.getenv().getOrDefault("ANTHROPIC_API_KEY", "").isBlank()
                ? new TemplateBugWriter() : ClaudeBugWriter.fromEnv();
        System.out.println(run(failures, writer, out));
    }

    static String run(List<FailureContext> failures, BugWriter writer, Path out) throws Exception {
        Files.createDirectories(out);
        List<BugReport> good = new ArrayList<>();
        StringBuilder log = new StringBuilder("Writer: " + writer.name() + "\n");
        int n = 0;
        for (FailureContext raw : failures) {
            n++;
            FailureContext safe = Redactor.redact(raw);
            BugReport report = writer.write(safe);
            ReportValidator.Result check = ReportValidator.validate(safe, report);
            String prefix = check.valid() ? "" : "NEEDS_HUMAN-";
            Files.writeString(out.resolve(prefix + "bug-" + n + ".md"), Exporters.markdown(report)
                    + (check.valid() ? "" : "\n> Validation problems: " + check.problems() + "\n"));
            if (check.valid()) {
                good.add(report);
            }
            log.append(check.valid() ? "OK          " : "NEEDS_HUMAN ").append(report.severity()).append("  ")
                    .append(report.title()).append(check.valid() ? "" : "  " + check.problems()).append("\n");
        }
        Files.writeString(out.resolve("jira-import.csv"), Exporters.jiraCsv(good));
        return log.toString();
    }
}
