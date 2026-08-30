package timey.ui.dashboard;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import javafx.application.Application;
import javafx.stage.Stage;
import timey.ApplicationFactory;
import timey.ui.CommandLineApp;
import timey.ui.ConsoleUi;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    private CommandLineApp commandLineApp;
    private StringWriter output;

    @Override
    public void init() {
        output = new StringWriter();
        commandLineApp = ApplicationFactory.createCommandLineApp(
                new ConsoleUi(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
    }

    @Override
    public void start(Stage stage) {
        MainWindow mainWindow = new MainWindow(stage, commandLineApp, output);
        mainWindow.show();
    }
}
