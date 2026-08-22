package Timey.ui.dashboard;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DashboardLauncherTest {
    @Test
    void dashboardStylesheet_packagedWithApplication_isAvailable() {
        assertNotNull(TimeyDashboardApp.class.getResource("/Timey/ui/dashboard/dashboard.css"));
    }
}
