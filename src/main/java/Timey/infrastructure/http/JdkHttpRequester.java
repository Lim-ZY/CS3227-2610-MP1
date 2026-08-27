package Timey.infrastructure.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** HTTP implementation using the JDK client with a bounded request timeout. */
public final class JdkHttpRequester implements HttpRequester {
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private final HttpClient client;
    private final Duration requestTimeout;

    public JdkHttpRequester() {
        this(DEFAULT_REQUEST_TIMEOUT);
    }

    /** Creates a requester with the supplied connection and request timeout. */
    public JdkHttpRequester(Duration requestTimeout) {
        if (requestTimeout.isZero() || requestTimeout.isNegative()) {
            throw new IllegalArgumentException("Request timeout must be positive.");
        }
        this.requestTimeout = requestTimeout;
        this.client = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    JdkHttpRequester(HttpClient client) {
        this.client = client;
        this.requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    }

    @Override
    public HttpResult get(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(response.statusCode(), response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not reach the external service.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("External request was interrupted.", exception);
        }
    }
}
