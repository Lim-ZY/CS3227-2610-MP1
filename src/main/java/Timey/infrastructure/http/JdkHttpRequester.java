package Timey.infrastructure.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** HTTP implementation using the JDK client with a bounded request timeout. */
public final class JdkHttpRequester implements HttpRequester {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private final HttpClient client;

    public JdkHttpRequester() {
        this(HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT).build());
    }

    JdkHttpRequester(HttpClient client) {
        this.client = client;
    }

    @Override
    public HttpResult get(URI uri, String authorization) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", authorization)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reach the location service.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Location lookup was interrupted.", exception);
        }
    }
}
