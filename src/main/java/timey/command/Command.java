package timey.command;

import timey.model.TimeyModel;

/** A parsed user action that can operate on Timey's application model. */
public abstract class Command {
    /** Executes this action against the current application model. */
    public abstract CommandResult execute(TimeyModel model);

    /** Returns whether executing this command ends the current session. */
    public boolean isExit() {
        return false;
    }
}
