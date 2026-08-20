package Timey.presentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

class CommandLineAppTest {
    @Test
    void displaysParsedPlanAndFarewell() {
        var outputText = new StringWriter();
        var app = new CommandLineApp(
                new BufferedReader(new StringReader("plan /from \"COM3\" /to \"VivoCity\" /by 1830 /buf 5m\nthx\n")),
                new PrintWriter(outputText, true));

        app.run();

        String output = outputText.toString();
        assertTrue(output.contains("Hey! I'll help you to be on track today as always!"));
        assertTrue(output.contains("> "));
        assertTrue(output.contains("From: COM3"));
        assertTrue(output.contains("To: VivoCity"));
        assertTrue(output.contains("Target arrival: 18:30"));
        assertTrue(output.contains("Personal buffer: 5 minutes"));
        assertTrue(output.contains("1. Fastest Transit — 43 minutes total"));
        assertTrue(output.contains("2. Direct Bus — 59 minutes total"));
        assertTrue(output.contains("Alrighty, hope you'll have a nice day ahead!"));
    }
}
