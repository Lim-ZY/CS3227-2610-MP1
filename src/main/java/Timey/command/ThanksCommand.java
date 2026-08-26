package Timey.command;

import Timey.model.TimeyModel;

/** Ends the current Timey session after acknowledging the user. */
public final class ThanksCommand extends Command {
    @Override
    public CommandResult execute(TimeyModel model) {
        return CommandResult.message("Alrighty, hope you'll have a nice day ahead!");
    }

    @Override
    public boolean isExit() {
        return true;
    }
}
