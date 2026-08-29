package timey.parser;

import java.time.Duration;
import java.util.HashMap;
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
        if (input == null || !input.startsWith("add")) {
            throw new IllegalArgumentException("Command must start with 'add'.");
        }
        String optionsText = input.substring("add".length());
        Matcher matcher = OPTION.matcher(optionsText);
        Map<String, String> options = new HashMap<>();
        int nextExpectedIndex = 0;
        while (matcher.find()) {
            if (!optionsText.substring(nextExpectedIndex, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Could not understand part of the add command.");
            }
            String optionName = matcher.group(1);
            if (options.put(optionName, valueOf(matcher)) != null) {
                throw new IllegalArgumentException("Option /" + optionName + " was provided more than once.");
            }
            nextExpectedIndex = matcher.end();
        }
        if (!optionsText.substring(nextExpectedIndex).isBlank()) {
            throw new IllegalArgumentException("Could not understand part of the add command.");
        }
        return new AddCommand(requiredOption(options, "from"), requiredOption(options, "to"),
                parseDuration(requiredOption(options, "dur")));
    }

    private String valueOf(Matcher matcher) {
        return matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
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
