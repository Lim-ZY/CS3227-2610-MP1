package timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class DashboardLauncherTest {
    @Test
    void dashboardStylesheet_packagedWithApplication_isAvailable() {
        assertNotNull(TimeyDashboardApp.class.getResource("/timey/ui/dashboard/dashboard.css"));
    }

    @Test
    void mainWindowLayout_packagedWithApplication_isAvailable() {
        assertNotNull(MainWindow.class.getResource("/timey/ui/dashboard/view/MainWindow.fxml"));
    }

    @Test
    void commandBarLayout_packagedWithApplication_isAvailable() {
        assertNotNull(CommandBar.class.getResource("/timey/ui/dashboard/view/CommandBar.fxml"));
    }

    @Test
    void commandOutputLayout_packagedWithApplication_isAvailable() {
        assertNotNull(CommandOutput.class.getResource("/timey/ui/dashboard/view/CommandOutput.fxml"));
    }

    @Test
    void dashboardHeaderLayout_packagedWithApplication_isAvailable() {
        assertNotNull(DashboardHeader.class.getResource("/timey/ui/dashboard/view/DashboardHeader.fxml"));
    }

    @Test
    void dashboardCardLayouts_packagedWithApplication_areAvailable() {
        assertNotNull(NextEventCard.class.getResource("/timey/ui/dashboard/view/NextEventCard.fxml"));
        assertNotNull(CommuteStatusCard.class.getResource("/timey/ui/dashboard/view/CommuteStatusCard.fxml"));
    }

    @Test
    void routeAlternativesLayout_packagedWithApplication_isAvailable() {
        assertNotNull(RouteAlternativesPanel.class.getResource("/timey/ui/dashboard/view/RouteAlternativesPanel.fxml"));
    }

    @Test
    void dashboardLayouts_delegateControllerOwnershipToTheirUiParts() throws IOException {
        for (String layout : List.of("MainWindow.fxml", "CommandBar.fxml", "CommandOutput.fxml",
                "DashboardHeader.fxml", "NextEventCard.fxml", "CommuteStatusCard.fxml",
                "RouteAlternativesPanel.fxml")) {
            assertFalse(fxmlContents(layout).contains("fx:controller"), layout);
        }
    }

    private String fxmlContents(String layout) throws IOException {
        URL resource = DashboardLauncherTest.class.getResource("/timey/ui/dashboard/view/" + layout);
        assertNotNull(resource);
        try (InputStream input = resource.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
