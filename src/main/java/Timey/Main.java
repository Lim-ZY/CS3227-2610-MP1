package Timey;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import Timey.presentation.CommandLineApp;

/** Entry point for the Timey application. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        var input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        var output = new PrintWriter(System.out, true);
        new CommandLineApp(input, output).run();
    }
}
