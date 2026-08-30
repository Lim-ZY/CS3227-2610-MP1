package timey.infrastructure.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WorkerErrorMessageParserTest {
    private static final String FALLBACK = "A safe fallback message.";

    @Test
    void extract_textualError_returnsTrimmedMessage() {
        String message = WorkerErrorMessageParser.extract("{\"error\":\"  Invalid query.  \"}", FALLBACK);

        assertEquals("Invalid query.", message);
    }

    @Test
    void extract_missingOrMalformedError_returnsFallback() {
        assertEquals(FALLBACK, WorkerErrorMessageParser.extract("{\"message\":\"Invalid query.\"}", FALLBACK));
        assertEquals(FALLBACK, WorkerErrorMessageParser.extract("not-json", FALLBACK));
    }

    @Test
    void extract_unsafeError_returnsFallback() {
        assertEquals(FALLBACK, WorkerErrorMessageParser.extract("{\"error\":\"Invalid\\nquery.\"}", FALLBACK));
        String excessivelyLongError = "{\"error\":\"" + "x".repeat(301) + "\"}";

        assertEquals(FALLBACK, WorkerErrorMessageParser.extract(excessivelyLongError, FALLBACK));
    }
}
