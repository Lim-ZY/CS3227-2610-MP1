package timey.ui.dashboard;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import timey.domain.transit.RouteAlternative;
import timey.ui.DashboardState;
import timey.ui.UiPart;

/** FXML-backed panel that displays route alternatives for the current commute plan. */
public final class RouteAlternativesPanel extends UiPart<VBox> {
    private static final String FXML = "RouteAlternativesPanel.fxml";

    @FXML
    private VBox alternatives;

    public RouteAlternativesPanel() {
        super(FXML);
    }

    /** Renders available routes, including the selected route and selection guidance. */
    public void render(DashboardState state) {
        alternatives.getChildren().setAll(routePanelLabel("ROUTE ALTERNATIVES"));
        if (state.alternatives().isEmpty()) {
            alternatives.getChildren().add(routePanelMessage(
                    "You can select one with `choose <number>` later."));
            return;
        }
        for (int index = 0; index < state.alternatives().size(); index++) {
            RouteAlternative route = state.alternatives().get(index);
            boolean selected = state.recommendation()
                    .map(recommendation -> recommendation.routeName().equals(route.name()))
                    .orElse(false);
            alternatives.getChildren().add(routeAlternative(index + 1, route, selected));
        }
        Label guidance = new Label("Select a route from the command bar, for example: choose 1");
        guidance.getStyleClass().add("route-guidance");
        alternatives.getChildren().add(guidance);
    }

    /** Displays the loading state while route alternatives are being requested. */
    public void showLoading() {
        showRoutePanelMessage("Loading route alternatives…");
    }

    /** Displays the fallback state when route lookup fails. */
    public void showFailure() {
        showRoutePanelMessage("Route lookup failed. Try the command again.");
    }

    private void showRoutePanelMessage(String text) {
        alternatives.getChildren().setAll(routePanelLabel("ROUTE ALTERNATIVES"), routePanelMessage(text));
    }

    private VBox routeAlternative(int routeNumber, RouteAlternative route, boolean selected) {
        Label name = new Label(routeNumber + ". " + route.name() + " · " + route.totalDuration().toMinutes() + " min"
                + (selected ? "  SELECTED" : ""));
        name.getStyleClass().add(selected ? "route-name-selected" : "route-name");
        Label details = new Label("Walk " + route.walkingDuration().toMinutes() + " min · Rail "
                + route.transitDuration().toMinutes() + " min · " + route.transferCount()
                + (route.transferCount() == 1 ? " transfer" : " transfers"));
        details.getStyleClass().add("muted");
        VBox alternative = new VBox(5, name, details);
        alternative.getStyleClass().add(selected ? "route-alternative-selected" : "route-alternative");
        return alternative;
    }

    private Label routePanelLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-label");
        return label;
    }

    private Label routePanelMessage(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }
}
