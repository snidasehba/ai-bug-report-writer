# AI Bug Report Writer

Hackathon-style weekend build. Turning a red test into a bug a developer can act on takes a tester
10–15 minutes: read the stack trace, dig through logs, write steps, pick a severity, and make sure no
card number or token ends up in Jira. This tool does the first draft with the **Claude API** and then
**checks the AI's work** before anything is filed.

Built with **Java 17**, the **Anthropic Java SDK** (structured output), **TestNG** and **GitHub Actions**.

## Pipeline

1. **Redact** the failure (error, stack trace, log excerpt, environment) before it leaves the machine:
   card numbers become `****1111`, bearer tokens / API keys / passwords become `[REDACTED]`, emails become `[EMAIL]`.
2. **Write** a structured bug: title, summary, steps to reproduce, expected vs actual, severity
   (BLOCKER / CRITICAL / MAJOR / MINOR), component and evidence. Claude is used when `ANTHROPIC_API_KEY`
   is set; otherwise a rule-based template writer runs, so the pipeline never depends on the API.
3. **Validate** the report like any untrusted input. It is rejected if:
   - an evidence quote does not appear in the (redacted) failure text, i.e. the model made it up;
   - it contains a full card number;
   - it includes a link that was not in the input;
   - the title is missing, too long or just the test name; the severity is not one of the four.
4. **Export** valid reports as Markdown and a **Jira import CSV**. Invalid ones are written as
   `NEEDS_HUMAN-bug-N.md` with the reasons, and kept out of the CSV.

Severity follows money impact: wrong balances, refunds or double charges are BLOCKER; a 5xx on a payment
flow is CRITICAL; formatting issues are MAJOR or MINOR.

## Example (template writer, the failures in `examples/failures.json`)

```
Writer: template
OK  BLOCKER   Partial refund reduces balance fails: expected [6000] but found [10000]
OK  CRITICAL  Pay with saved card fails: Expected status 200 but was 502
OK  MAJOR     Balance shows two decimals fails: expected: <AED 120.50> but was: <AED 120.5>
```

Evidence in the second report, after redaction:

```
POST /v1/charges card=****1111 Authorization: Bearer [REDACTED] -> 502
ERROR payment-gateway upstream timeout after 30000ms customer=[EMAIL]
```

## Run it

```bash
mvn package
java -jar target/ai-bug-report-writer.jar examples/failures.json bug-reports

export ANTHROPIC_API_KEY=...   # optional: use Claude instead of the template writer
java -jar target/ai-bug-report-writer.jar examples/failures.json bug-reports
```

## Tests

13 TestNG tests: redaction (cards, tokens, keys, emails, and ordinary numbers left alone), the validator
(invented evidence, leaked card numbers, made-up links, bad severity), the template writer, the
end-to-end run including "no secret reaches the CSV", and a live Claude test that runs only when an API
key is configured.
