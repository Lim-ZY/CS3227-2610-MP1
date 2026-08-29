package timey.ui.dashboard;

import static java.util.Objects.requireNonNull;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import timey.ui.UiPart;

/** FXML-backed command entry component for the dashboard. */
public final class CommandBar extends UiPart<HBox> {
    private static final String FXML = "CommandBar.fxml";
    private static final String DEFAULT_PROMPT = "Enter a Timey command, for example: plan /from \"COM3\" "
            + "/to \"VivoCity\" /by 1830";
    private static final String READY_PROMPT = "Enter a Timey command, for example: choose 1";

    private CommandExecutor commandExecutor;

    @FXML
    private TextField commandTextField;

    public CommandBar() {
        super(FXML);
    }

    /** Sets the callback to invoke after the user submits a non-blank command. */
    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = requireNonNull(commandExecutor);
    }

    /** Marks the command session as ended and leaves entry disabled. */
    public void showSessionEnded() {
        commandTextField.setPromptText("This command session has ended");
    }

    /** Re-enables entry after a command completes successfully. */
    public void showReadyAfterSuccess() {
        commandTextField.setDisable(false);
        commandTextField.setPromptText(READY_PROMPT);
    }

    /** Re-enables entry after a command fails. */
    public void showReadyAfterFailure() {
        commandTextField.setDisable(false);
        commandTextField.setPromptText(DEFAULT_PROMPT);
    }

    @FXML
    private void handleCommandEntered() {
        String commandText = commandTextField.getText();
        if (commandText.isBlank()) {
            return;
        }
        commandTextField.clear();
        commandTextField.setDisable(true);
        commandTextField.setPromptText("Updating your commute…");
        requireNonNull(commandExecutor, "A command executor must be configured before command entry.")
                .execute(commandText);
    }

    /** Handles a command submitted from the dashboard command bar. */
    @FunctionalInterface
    public interface CommandExecutor {
        void execute(String commandText);
    }
}
