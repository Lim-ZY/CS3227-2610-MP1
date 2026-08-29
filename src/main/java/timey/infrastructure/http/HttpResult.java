package timey.infrastructure.http;

/** HTTP response data required by Timey's external API adapters. */
public record HttpResult(int statusCode, String body) {
}
