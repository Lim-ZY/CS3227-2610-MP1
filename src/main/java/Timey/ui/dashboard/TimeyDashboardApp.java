package Timey.ui.dashboard;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import Timey.ApplicationFactory;
import Timey.ui.CommandLineApp;
import Timey.ui.ConsoleUi;
import javafx.application.Application;
import javafx.stage.Stage;

/** JavaFX presentation shell for Timey's dashboard. */
public final class TimeyDashboardApp extends Application {
    @Override
    public void start(Stage stage) {
        StringWriter output = new StringWriter();
        CommandLineApp commandLineApp = ApplicationFactory.createCommandLineApp(
                new ConsoleUi(new BufferedReader(new StringReader("")), new PrintWriter(output, true)));
        MainWindow mainWindow = new MainWindow(stage, commandLineApp, output, ApplicationFactory.loadUserPreferences());
        mainWindow.show();
    }
}
