package timey.command;

import java.util.List;

/** Feedback produced by executing a command for presentation by the UI. */
public record CommandResult(List<String> messages) {
    public CommandResult {
        messages = List.copyOf(messages);
    }

    /** Creates a result containing one line of feedback. */
    public static CommandResult message(String message) {
        return new CommandResult(List.of(message));
    }
}
