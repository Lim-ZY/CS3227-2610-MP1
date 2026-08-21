package Timey.infrastructure.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import Timey.infrastructure.http.HttpResult;

class OneMapLocationResolverTest {
    @Test
    void resolve_successfulOneMapResponse_returnsFirstLocation() {
        var resolver = new OneMapLocationResolver((uri, authorization) -> {
            assertTrue(uri.toString().contains("searchVal=VivoCity"));
            assertEquals("access-token", authorization);
            return new HttpResult(200, """
                    {"results":[{"SEARCHVAL":"VivoCity","ADDRESS":"1 HarbourFront Walk, Singapore 098585",
                    "LATITUDE":"1.2645","LONGITUDE":"103.8224"}]}""");
        }, Optional.of("access-token"));

        var result = resolver.resolve("VivoCity");

        assertTrue(result.isFound());
        assertEquals("VivoCity", result.location().orElseThrow().displayName());
        assertEquals(1.2645, result.location().orElseThrow().latitude());
    }

    @Test
    void resolve_tokenMissing_doesNotAccessNetwork() {
        var resolver = new OneMapLocationResolver((uri, authorization) -> {
            throw new AssertionError("No request should be made without a token.");
        }, Optional.empty());

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap lookup is not configured.", result.reason());
    }

    @Test
    void resolve_blankQuery_doesNotAccessNetwork() {
        var resolver = new OneMapLocationResolver((uri, authorization) -> {
            throw new AssertionError("No request should be made for a blank query.");
        }, Optional.of("access-token"));

        var result = resolver.resolve("  ");

        assertFalse(result.isFound());
        assertEquals("A location is required for online lookup.", result.reason());
    }

    @Test
    void resolve_incompleteProviderResponse_returnsNotFoundReason() {
        var resolver = new OneMapLocationResolver((uri, authorization) -> new HttpResult(200,
                "{\"results\":[{\"SEARCHVAL\":\"COM3\"}]}"), Optional.of("access-token"));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap could not find \"COM3\".", result.reason());
    }

    @Test
    void resolve_providerFailure_returnsFallbackReason() {
        var resolver = new OneMapLocationResolver((uri, authorization) -> new HttpResult(429, "{}"),
                Optional.of("access-token"));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap lookup is temporarily unavailable (HTTP 429).", result.reason());
    }

    @Test
    void resolve_requestFailure_returnsTemporaryUnavailableReason() {
        var resolver = new OneMapLocationResolver((uri, authorization) -> {
            throw new IllegalStateException("Connection timed out");
        }, Optional.of("access-token"));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap lookup is temporarily unavailable.", result.reason());
    }
}
