package timey.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Extracts a safe error message from a Worker JSON response. */
public final class WorkerErrorMessageParser {
    private static final int MAXIMUM_MESSAGE_LENGTH = 300;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private WorkerErrorMessageParser() {
    }

    /** Returns the response error message when it is safe to display, otherwise the supplied fallback. */
    public static String extract(String responseBody, String fallback) {
        if (fallback == null || fallback.isBlank()) {
            throw new IllegalArgumentException("Fallback message must not be blank.");
        }
        try {
            JsonNode error = OBJECT_MAPPER.readTree(responseBody).path("error");
            if (!error.isTextual()) {
                return fallback;
            }
            String message = error.asText().strip();
            return isSafe(message) ? message : fallback;
        } catch (JsonProcessingException | NullPointerException exception) {
            return fallback;
        }
    }

    private static boolean isSafe(String message) {
        return !message.isBlank() && message.length() <= MAXIMUM_MESSAGE_LENGTH
                && message.chars().noneMatch(Character::isISOControl);
    }
}
