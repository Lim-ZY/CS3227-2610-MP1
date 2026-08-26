package Timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import Timey.model.TimeyModel;

class ThanksCommandTest {
    @Test
    void execute_returnsFarewellAndEndsSession() {
        var command = new ThanksCommand();

        var result = command.execute(new TimeyModel());

        assertEquals(java.util.List.of("Alrighty, hope you'll have a nice day ahead!"), result.messages());
        assertTrue(command.isExit());
    }
}
