package timey.parser;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timey.command.AddCommand;

/** Parses {@code add /from <source> /to <destination> /dur 1h30m} commands. */
public final class AddCommandParser {
    private static final Pattern OPTION = Pattern.compile("/(from|to|dur)\\s+(?:\\\"([^\\\"]+)\\\"|(\\S+))");
    private static final Pattern DURATION = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?");

    /** Performs this operation. */
    public AddCommand parse(String input) {
        Map<String, String> options = CommandOptionParser.parse(input, "add", OPTION);
        return new AddCommand(requiredOption(options, "from"), requiredOption(options, "to"),
                parseDuration(requiredOption(options, "dur")));
    }

    private String requiredOption(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option /" + name + ".");
        }
        return value;
    }

    private Duration parseDuration(String value) {
        Matcher matcher = DURATION.matcher(value);
        if (!matcher.matches() || (matcher.group(1) == null && matcher.group(2) == null)) {
            throw new IllegalArgumentException("Duration must use hours and minutes, for example 1h30m.");
        }
        try {
            long hours = matcher.group(1) == null ? 0 : Long.parseLong(matcher.group(1));
            long minutes = matcher.group(2) == null ? 0 : Long.parseLong(matcher.group(2));
            Duration duration = Duration.ofHours(hours).plusMinutes(minutes);
            if (duration.isZero()) {
                throw new IllegalArgumentException("Duration must be greater than zero.");
            }
            return duration;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Duration is too large.");
        }
    }
}
