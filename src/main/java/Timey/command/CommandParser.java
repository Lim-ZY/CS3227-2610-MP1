package Timey.command;

/** Parses a complete user command into the action requested by the user. */
public final class CommandParser {
    private final PlanCommandParser planCommandParser;

    public CommandParser(PlanCommandParser planCommandParser) {
        this.planCommandParser = planCommandParser;
    }

    /**
     * Parses a user command. Plan commands are fully validated by {@link PlanCommandParser}.
     *
     * @param input command text entered by the user
     * @return the requested command action
     * @throws IllegalArgumentException when a plan command is invalid
     */
    public Command parse(String input) {
        String command = input.trim();
        if (command.equalsIgnoreCase("thx")) {
            return Command.thanks();
        }
        if (command.startsWith("plan")) {
            return Command.plan(planCommandParser.parse(command));
        }
        if (command.startsWith("choose")) {
            return Command.choose(parseNumberArgument(command));
        }
        if (command.equalsIgnoreCase("reminders")) {
            return Command.reminders();
        }
        if (command.startsWith("cancel")) {
            return Command.cancel(parseNumberArgument(command));
        }
        return Command.unknown();
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

    /** A parsed command and any arguments required to execute it. */
    public record Command(Type type, PlanCommand plan, Integer number) {
        public static Command thanks() {
            return new Command(Type.THANKS, null, null);
        }

        public static Command plan(PlanCommand plan) {
            return new Command(Type.PLAN, plan, null);
        }

        public static Command choose(Integer number) {
            return new Command(Type.CHOOSE, null, number);
        }

        public static Command reminders() {
            return new Command(Type.REMINDERS, null, null);
        }

        public static Command cancel(Integer number) {
            return new Command(Type.CANCEL, null, number);
        }

        public static Command unknown() {
            return new Command(Type.UNKNOWN, null, null);
        }
    }

    /** The actions accepted by the command-line interface. */
    public enum Type {
        THANKS,
        PLAN,
        CHOOSE,
        REMINDERS,
        CANCEL,
        UNKNOWN
    }
}
