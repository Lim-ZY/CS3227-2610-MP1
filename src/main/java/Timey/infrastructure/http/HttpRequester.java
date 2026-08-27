package Timey.infrastructure.http;

import java.net.URI;

/** Small HTTP boundary that keeps API adapters unit-testable without network access. */
public interface HttpRequester {
    HttpResult get(URI uri);
}
