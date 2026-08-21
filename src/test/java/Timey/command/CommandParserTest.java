package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CommandParserTest {
    private final CommandParser parser = new CommandParser(new PlanCommandParser());

    @Test
    void parse_planCommand_returnsValidatedPlanAction() {
        var command = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertEquals(CommandParser.Type.PLAN, command.type());
        assertEquals("COM3", command.plan().origin());
    }

    @Test
    void parse_numberedCommands_returnsNumberWhenPresent() {
        assertEquals(2, parser.parse("choose 2").number());
        assertEquals(1, parser.parse("cancel 1").number());
    }

    @Test
    void parse_malformedNumberedCommand_returnsActionWithoutNumber() {
        assertEquals(CommandParser.Type.CHOOSE, parser.parse("choose one").type());
        assertNull(parser.parse("choose one").number());
        assertEquals(CommandParser.Type.CANCEL, parser.parse("cancel 1 2").type());
        assertNull(parser.parse("cancel 1 2").number());
    }

    @Test
    void parse_nonArgumentCommands_classifiesActions() {
        assertEquals(CommandParser.Type.THANKS, parser.parse("thx").type());
        assertEquals(CommandParser.Type.REMINDERS, parser.parse("reminders").type());
        assertEquals(CommandParser.Type.UNKNOWN, parser.parse("help").type());
    }
}
