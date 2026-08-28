package Timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DashboardLauncherTest {
    @Test
    void dashboardStylesheet_packagedWithApplication_isAvailable() {
        assertNotNull(TimeyDashboardApp.class.getResource("/Timey/ui/dashboard/dashboard.css"));
    }

    @Test
    void mainWindowLayout_packagedWithApplication_isAvailable() {
        assertNotNull(MainWindow.class.getResource("/Timey/ui/dashboard/view/MainWindow.fxml"));
    }

    @Test
    void commandBarLayout_packagedWithApplication_isAvailable() {
        assertNotNull(CommandBar.class.getResource("/Timey/ui/dashboard/view/CommandBar.fxml"));
    }
}
