package timey.ui;

/** Provides a safe recovery message when a command cannot complete. */
public final class CommandFailureText {
    private static final String RUNTIME_FAILURE = "Timey could not complete that command. Your current plan has not "
            + "changed. Check your internet connection or saved data, then try again.";

    private CommandFailureText() {
    }

    /** Returns recovery guidance without exposing an internal failure. */
    public static String runtimeFailure() {
        return RUNTIME_FAILURE;
    }
}
