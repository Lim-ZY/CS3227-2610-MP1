package timey.parser;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import timey.command.PlanCommand;

/** Parses the CLI syntax used to request a commute plan. */
public final class PlanCommandParser {
    private static final Duration DEFAULT_BUFFER = Duration.ofMinutes(10);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");
    private static final Pattern OPTION = Pattern.compile("/(from|to|by|buf)\\s+(?:\\\"([^\\\"]+)\\\"|(\\S+))");
    private final Duration defaultBuffer;

    public PlanCommandParser() {
        this(DEFAULT_BUFFER);
    }

    /** Creates a parser with the persisted buffer used when /buf is omitted. */
    public PlanCommandParser(Duration defaultBuffer) {
        if (defaultBuffer == null || defaultBuffer.isNegative()) {
            throw new IllegalArgumentException("Default buffer must not be negative.");
        }
        this.defaultBuffer = defaultBuffer;
    }

    /**
     * Parses a plan request in the form {@code plan /from "COM3" /to "VivoCity" /by 1830 /buf 10m}.
     *
     * @param input command text entered by the user
     * @return the validated request
     * @throws IllegalArgumentException when the command is not a valid plan request
     */
    public PlanCommand parse(String input) {
        Map<String, String> options = CommandOptionParser.parse(input, "plan", OPTION);

        String origin = requiredOption(options, "from");
        String destination = requiredOption(options, "to");
        LocalTime arrivalTime = parseTime(requiredOption(options, "by"));
        Duration buffer = options.containsKey("buf") ? parseBuffer(options.get("buf")) : defaultBuffer;
        return new PlanCommand(origin, destination, arrivalTime, buffer);
    }

    private String requiredOption(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option /" + name + ".");
        }
        return value;
    }

    private LocalTime parseTime(String value) {
        if (!value.matches("\\d{4}")) {
            throw new IllegalArgumentException("Arrival time must use 24-hour HHmm format, for example 1830.");
        }
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Arrival time must be a valid 24-hour time.");
        }
    }

    private Duration parseBuffer(String value) {
        Matcher matcher = Pattern.compile("(\\d+)m").matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Buffer must be a whole number of minutes, for example 10m.");
        }
        try {
            return Duration.ofMinutes(Long.parseLong(matcher.group(1)));
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Buffer is too large.");
        }
    }
}
