package timey.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses the option arguments shared by Timey's command parsers. */
final class CommandOptionParser {
    private CommandOptionParser() {
    }

    static Map<String, String> parse(String input, String commandName, Pattern optionPattern) {
        if (!hasCommandName(input, commandName)) {
            throw new IllegalArgumentException("Command must start with '" + commandName + "'.");
        }

        String optionsText = input.substring(commandName.length());
        Matcher matcher = optionPattern.matcher(optionsText);
        Map<String, String> options = new HashMap<>();
        int nextExpectedIndex = 0;
        while (matcher.find()) {
            if (!optionsText.substring(nextExpectedIndex, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Could not understand part of the " + commandName + " command.");
            }
            String optionName = matcher.group(1);
            if (options.put(optionName, valueOf(matcher)) != null) {
                throw new IllegalArgumentException("Option /" + optionName + " was provided more than once.");
            }
            nextExpectedIndex = matcher.end();
        }
        if (!optionsText.substring(nextExpectedIndex).isBlank()) {
            throw new IllegalArgumentException("Could not understand part of the " + commandName + " command.");
        }
        return options;
    }

    static String requiredOption(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option /" + name + ".");
        }
        return value;
    }

    static boolean hasCommandName(String input, String commandName) {
        if (input == null || !input.regionMatches(true, 0, commandName, 0, commandName.length())) {
            return false;
        }
        return input.length() == commandName.length()
                || Character.isWhitespace(input.charAt(commandName.length()));
    }

    private static String valueOf(Matcher matcher) {
        return matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
    }
}
