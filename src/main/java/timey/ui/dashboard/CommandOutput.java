package timey.ui.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import timey.ui.CommandFailureText;
import timey.ui.UiPart;

/** FXML-backed transcript display for dashboard command results. */
public final class CommandOutput extends UiPart<TextArea> {
    private static final String FXML = "CommandOutput.fxml";
    private static final String INITIAL_MESSAGE = "Use the command bar below, for example:\n"
            + "plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 10m";

    @FXML
    private TextArea commandOutput;

    /** Creates a new CommandOutput. */
    public CommandOutput() {
        super(FXML);
        commandOutput.setText(INITIAL_MESSAGE);
    }

    /** Appends output produced by a successfully executed command. */
    public void appendCommandResult(String commandText, String resultText) {
        commandOutput.appendText("\n> " + commandText + "\n" + resultText);
    }

    /** Appends the dashboard's fallback message for a failed command execution. */
    public void appendCommandFailure(String commandText) {
        commandOutput.appendText("\n> " + commandText + "\n" + CommandFailureText.runtimeFailure());
    }
}
