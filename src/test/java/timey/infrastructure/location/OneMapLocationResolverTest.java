package timey.infrastructure.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import timey.infrastructure.http.HttpResult;

class OneMapLocationResolverTest {
    @Test
    void resolve_successfulOneMapResponse_returnsFirstLocation() {
        var resolver = new OneMapLocationResolver(uri -> {
            assertEquals("https://timey.example.workers.dev/v1/search?q=VivoCity", uri.toString());
            return new HttpResult(200, """
                    {"results":[{"SEARCHVAL":"VivoCity","ADDRESS":"1 HarbourFront Walk, Singapore 098585",
                    "LATITUDE":"1.2645","LONGITUDE":"103.8224"}]}""");
        }, Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("VivoCity");

        assertTrue(result.isFound());
        assertEquals("VivoCity", result.location().orElseThrow().displayName());
        assertEquals(1.2645, result.location().orElseThrow().latitude());
    }

    @Test
    void resolve_numericCoordinates_returnsLocation() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, """
                {"results":[{"SEARCHVAL":"VivoCity","ADDRESS":"1 HarbourFront Walk",
                "LATITUDE":1.2645,"LONGITUDE":103.8224}]}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("VivoCity");

        assertTrue(result.isFound());
        assertEquals(103.8224, result.location().orElseThrow().longitude());
    }

    @Test
    void resolve_queryNeedsEncoding_encodesQueryBeforeRequest() {
        var resolver = new OneMapLocationResolver(uri -> {
            assertEquals("https://timey.example.workers.dev/v1/search?q=NUS+%2F+COM3", uri.toString());
            return new HttpResult(200, "{\"results\":[]}");
        }, Optional.of(URI.create("https://timey.example.workers.dev")));

        resolver.resolve("NUS / COM3");
    }

    @Test
    void resolve_serviceNotConfigured_doesNotAccessNetwork() {
        var resolver = new OneMapLocationResolver(uri -> {
            throw new AssertionError("No request should be made without a service URL.");
        }, Optional.empty());

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("Live location lookup is not configured.", result.reason());
    }

    @Test
    void resolve_blankQuery_doesNotAccessNetwork() {
        var resolver = new OneMapLocationResolver(uri -> {
            throw new AssertionError("No request should be made for a blank query.");
        }, Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("  ");

        assertFalse(result.isFound());
        assertEquals("A location is required for online lookup.", result.reason());
    }

    @Test
    void resolve_incompleteProviderResponse_returnsNotFoundReason() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200,
                "{\"results\":[{\"SEARCHVAL\":\"COM3\"}]}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap could not find \"COM3\".", result.reason());
    }

    @Test
    void resolve_malformedProviderResponse_returnsUnreadableResponseReason() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, "not-json"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap lookup returned an unreadable response.", result.reason());
    }

    @Test
    void resolve_resultsSplitAcrossRecords_doesNotCombineThemIntoALocation() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, """
                {"results":[{"SEARCHVAL":"COM3"},{"ADDRESS":"13 Computing Drive",
                "LATITUDE":"1.294","LONGITUDE":"103.773"}]}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap could not find \"COM3\".", result.reason());
    }

    @Test
    void resolve_invalidCoordinates_returnsNotFoundReason() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, """
                {"results":[{"SEARCHVAL":"COM3","ADDRESS":"13 Computing Drive",
                "LATITUDE":"91","LONGITUDE":"103.773"}]}"""),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap could not find \"COM3\".", result.reason());
    }

    @Test
    void resolve_multipleResults_returnsFirstLocation() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, """
                {"results":[
                {"SEARCHVAL":"COM3 Cafe","ADDRESS":"First address","LATITUDE":"1.294","LONGITUDE":"103.773"},
                {"SEARCHVAL":"COM3 Hall","ADDRESS":"Second address","LATITUDE":"1.295","LONGITUDE":"103.774"}
                ]}"""), Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertTrue(result.isFound());
        assertEquals("COM3 Cafe", result.location().orElseThrow().displayName());
        assertEquals("First address", result.location().orElseThrow().address());
    }

    @Test
    void resolve_laterExactMatchAmongMultipleResults_returnsFirstLocation() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, """
                {"results":[
                {"SEARCHVAL":"COM3 Cafe","ADDRESS":"First address","LATITUDE":"1.294","LONGITUDE":"103.773"},
                {"SEARCHVAL":"COM3","ADDRESS":"13 Computing Drive","LATITUDE":"1.295","LONGITUDE":"103.774"}
                ]}"""), Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertTrue(result.isFound());
        assertEquals("First address", result.location().orElseThrow().address());
    }

    @Test
    void resolve_invalidResultBeforeValidResult_returnsFirstValidLocation() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(200, """
                {"results":[
                {"SEARCHVAL":"COM3","ADDRESS":"","LATITUDE":"1.294","LONGITUDE":"103.773"},
                {"SEARCHVAL":"COM3 Cafe","ADDRESS":"First valid address",
                "LATITUDE":"1.295","LONGITUDE":"103.774"}
                ]}"""), Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertTrue(result.isFound());
        assertEquals("COM3 Cafe", result.location().orElseThrow().displayName());
        assertEquals("First valid address", result.location().orElseThrow().address());
    }

    @Test
    void resolve_providerFailure_returnsFallbackReason() {
        var resolver = new OneMapLocationResolver(uri -> new HttpResult(429, "{}"),
                Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap lookup is temporarily unavailable (HTTP 429).", result.reason());
    }

    @Test
    void resolve_requestFailure_returnsTemporaryUnavailableReason() {
        var resolver = new OneMapLocationResolver(uri -> {
            throw new IllegalStateException("Connection timed out");
        }, Optional.of(URI.create("https://timey.example.workers.dev")));

        var result = resolver.resolve("COM3");

        assertFalse(result.isFound());
        assertEquals("OneMap lookup is temporarily unavailable.", result.reason());
    }
}
