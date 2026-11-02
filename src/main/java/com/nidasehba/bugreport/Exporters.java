package com.nidasehba.bugreport;

import java.util.List;
import java.util.stream.Collectors;

/** Markdown for humans and a CSV that Jira's issue importer accepts. */
public final class Exporters {

    private Exporters() {
    }

    public static String markdown(BugReport r) {
        StringBuilder md = new StringBuilder("## ").append(r.title()).append("\n\n");
        md.append("**Severity:** ").append(r.severity()).append("  \n**Component:** ").append(r.component()).append("\n\n");
        md.append(r.summary()).append("\n\n### Steps to reproduce\n");
        for (int i = 0; i < r.stepsToReproduce().size(); i++) {
            md.append(i + 1).append(". ").append(r.stepsToReproduce().get(i)).append("\n");
        }
        md.append("\n**Expected:** ").append(r.expectedResult()).append("\n\n**Actual:** ").append(r.actualResult()).append("\n\n### Evidence\n");
        r.evidence().forEach(e -> md.append("```\n").append(e).append("\n```\n"));
        return md.toString();
    }

    public static String jiraCsv(List<BugReport> reports) {
        String header = "Summary,Issue Type,Priority,Component/s,Description";
        if (reports.isEmpty()) {
            return header + "\n";
        }
        return reports.stream().map(r -> String.join(",", csv(r.title()), "Bug", csv(priority(r.severity())),
                        csv(r.component()), csv(markdown(r))))
                .collect(Collectors.joining("\n", header + "\n", "\n"));
    }

    static String priority(String severity) {
        return switch (severity) {
            case "BLOCKER" -> "Highest";
            case "CRITICAL" -> "High";
            case "MAJOR" -> "Medium";
            default -> "Low";
        };
    }

    static String csv(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
