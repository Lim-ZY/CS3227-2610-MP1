package timey.parser;

import timey.command.ChooseCommand;
import timey.command.Command;
import timey.command.HelpCommand;
import timey.command.ListCommand;
import timey.command.RemoveCommand;
import timey.command.ThanksCommand;
import timey.command.UnknownCommand;

/** Parses a complete user command into the action requested by the user. */
public final class Parser {
    private final PlanCommandParser planCommandParser;
    private final AddCommandParser addCommandParser;

    public Parser(PlanCommandParser planCommandParser) {
        this(planCommandParser, new AddCommandParser());
    }

    /** Creates a new Parser. */
    public Parser(PlanCommandParser planCommandParser, AddCommandParser addCommandParser) {
        this.planCommandParser = planCommandParser;
        this.addCommandParser = addCommandParser;
    }

    /**
     * Parses a user command. Plan commands are fully validated by {@link PlanCommandParser}.
     *
     * @param input command text entered by the user
     * @return the requested command action
     * @throws IllegalArgumentException when a plan command is invalid
     */
    public Command parse(String input) {
        if (input == null) {
            return new UnknownCommand();
        }
        String command = input.trim();
        if (command.equalsIgnoreCase("thx")) {
            return new ThanksCommand();
        }
        if (command.equalsIgnoreCase("help")) {
            return new HelpCommand();
        }
        if (startsWithCommandName(command, "plan")) {
            return planCommandParser.parse(command);
        }
        if (startsWithCommandName(command, "add")) {
            return addCommandParser.parse(command);
        }
        if (command.equalsIgnoreCase("ls saved")) {
            return new ListCommand(ListCommand.ListType.SAVED);
        }
        if (command.equalsIgnoreCase("ls plans")) {
            return new ListCommand(ListCommand.ListType.PLANS);
        }
        if (startsWithCommandName(command, "rm")) {
            return new RemoveCommand(parseNumberArgument(command));
        }
        if (startsWithCommandName(command, "choose")) {
            return new ChooseCommand(parseNumberArgument(command));
        }
        return new UnknownCommand();
    }

    private boolean startsWithCommandName(String input, String commandName) {
        if (!input.regionMatches(true, 0, commandName, 0, commandName.length())) {
            return false;
        }
        return input.length() == commandName.length()
                || Character.isWhitespace(input.charAt(commandName.length()));
    }

    private Integer parseNumberArgument(String command) {
        String[] parts = command.split("\\s+");
        if (parts.length != 2) {
            return null;
        }
        try {
            return Integer.valueOf(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
