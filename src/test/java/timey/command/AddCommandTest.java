package timey.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import timey.TestTimeyModelFactory;
import timey.infrastructure.transit.InMemoryFixedCommuteStore;

class AddCommandTest {
    @Test
    void execute_savesFixedCommuteAndReturnsFeedback() {
        var fixedCommutes = new InMemoryFixedCommuteStore();
        var command = new AddCommand("COM3", "VivoCity", Duration.ofMinutes(90));

        var result = command.execute(TestTimeyModelFactory.create(fixedCommutes));

        assertEquals(Duration.ofMinutes(90), fixedCommutes.find("COM3", "VivoCity").orElseThrow().duration());
        assertEquals("Saved fixed timing from COM3 to VivoCity: 90 minutes.", result.messages().getFirst());
    }

    @Test
    void execute_sameCaseInsensitiveRouteAndDuration_returnsAlreadySavedMessage() {
        var fixedCommutes = new InMemoryFixedCommuteStore();
        new AddCommand("com3", "home", Duration.ofMinutes(90))
                .execute(TestTimeyModelFactory.create(fixedCommutes));

        var result = new AddCommand("COM3", "Home", Duration.ofMinutes(90))
                .execute(TestTimeyModelFactory.create(fixedCommutes));

        assertIterableEquals(java.util.List.of(
                "This route has already been saved for you! Do check it out", "using `ls saved`."), result.messages());
        assertEquals("com3", fixedCommutes.findAll().getFirst().origin());
    }

    @Test
    void execute_sameCaseInsensitiveRouteWithDifferentDuration_changesSavedTiming() {
        var fixedCommutes = new InMemoryFixedCommuteStore();
        new AddCommand("com3", "home", Duration.ofMinutes(90))
                .execute(TestTimeyModelFactory.create(fixedCommutes));

        var result = new AddCommand("COM3", "Home", Duration.ofMinutes(100))
                .execute(TestTimeyModelFactory.create(fixedCommutes));

        assertEquals("Changed fixed timing from COM3 to Home: 100 minutes.", result.messages().getFirst());
        assertEquals(Duration.ofMinutes(100), fixedCommutes.find("com3", "home").orElseThrow().duration());
    }
}
