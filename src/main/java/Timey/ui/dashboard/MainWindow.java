package Timey.ui.dashboard;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import Timey.ui.UiPart;

/** Main FXML-backed window that hosts Timey's dashboard UI parts. */
public final class MainWindow extends UiPart<Stage> {
    private static final String FXML = "MainWindow.fxml";

    @FXML
    private StackPane headerPlaceholder;
    @FXML
    private StackPane dashboardPlaceholder;
    @FXML
    private StackPane commandBarPlaceholder;

    public MainWindow(Stage primaryStage) {
        super(FXML, primaryStage);
    }

    /** Displays the window after its UI parts have been added. */
    public void show() {
        getRoot().show();
    }

    /** Places the dashboard header in its FXML placeholder. */
    public void setHeader(Node header) {
        headerPlaceholder.getChildren().setAll(header);
    }

    /** Places the dashboard content in its FXML placeholder. */
    public void setDashboardContent(Node dashboardContent) {
        dashboardPlaceholder.getChildren().setAll(dashboardContent);
    }

    /** Places the dashboard command bar in its FXML placeholder. */
    public void setCommandBar(Node commandBar) {
        commandBarPlaceholder.getChildren().setAll(commandBar);
    }

    /** Registers cleanup to run when the window is hidden. */
    public void setOnHidden(EventHandler<WindowEvent> handler) {
        getRoot().setOnHidden(handler);
    }
}
