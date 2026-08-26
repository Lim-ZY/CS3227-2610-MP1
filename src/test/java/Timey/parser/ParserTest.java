package Timey.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ParserTest {
    private final Parser parser = new Parser(new PlanCommandParser());

    @Test
    void parse_planCommand_returnsValidatedPlanAction() {
        var command = parser.parse("plan /from \"COM3\" /to \"VivoCity\" /by 1830");

        assertEquals(Parser.Type.PLAN, command.type());
        assertEquals("COM3", command.plan().origin());
    }

    @Test
    void parse_addCommand_returnsValidatedFixedTimingAction() {
        var command = parser.parse("add /from COM3 /to VivoCity /dur 1h30m");

        assertEquals(Parser.Type.ADD, command.type());
        assertEquals(java.time.Duration.ofMinutes(90), command.addTiming().duration());
    }

    @Test
    void parse_numberedCommands_returnsNumberWhenPresent() {
        assertEquals(2, parser.parse("choose 2").number());
        assertEquals(1, parser.parse("cancel 1").number());
    }

    @Test
    void parse_malformedNumberedCommand_returnsActionWithoutNumber() {
        assertEquals(Parser.Type.CHOOSE, parser.parse("choose one").type());
        assertNull(parser.parse("choose one").number());
        assertEquals(Parser.Type.CANCEL, parser.parse("cancel 1 2").type());
        assertNull(parser.parse("cancel 1 2").number());
    }

    @Test
    void parse_nonArgumentCommands_classifiesActions() {
        assertEquals(Parser.Type.THANKS, parser.parse("thx").type());
        assertEquals(Parser.Type.REMINDERS, parser.parse("reminders").type());
        assertEquals(Parser.Type.UNKNOWN, parser.parse("help").type());
    }
}
