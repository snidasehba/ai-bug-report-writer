package com.nidasehba.bugreport;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class RedactorTest {

    @Test
    public void masksCardNumbersKeepingTheLastFourDigits() {
        String out = Redactor.redact("card=4111 1111 1111 1111 and 5500-0000-0000-0004");
        assertEquals(out, "card=****1111 and ****0004");
        assertFalse(Redactor.containsCardNumber(out));
    }

    @Test
    public void masksTokensKeysAndEmails() {
        String out = Redactor.redact("Authorization: Bearer sk_test_51HxAbCdEf api_key=abcd1234 \"password\": \"hunter22\" by jane.doe@example.com");
        assertFalse(out.contains("sk_test_51HxAbCdEf"));
        assertFalse(out.contains("abcd1234"));
        assertFalse(out.contains("hunter22"));
        assertFalse(out.contains("jane.doe"));
        assertTrue(out.contains("Bearer [REDACTED]"));
        assertTrue(out.contains("[EMAIL]"));
    }

    @Test
    public void leavesOrdinaryNumbersAlone() {
        String text = "expected [6000] but found [10000], build 2026.11.02-3, order 123456";
        assertEquals(Redactor.redact(text), text);
    }
}
