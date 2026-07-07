package boundary;
import utility.Menu;
import utility.MenuItem;

public class LoyaltyUI {
    private static final String TITLE = "Loyalty and Rewards Service ";
    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu", () -> {}
        ),
        PROFILE("Manage Profile", () -> {} //To Customers
        ),
        POINTS_AND_ACCUMULATION("Manage Your Points", () -> {} //Add redemption processing, redemption requests and expiring alert.
        ),
        REDEMPTION("Redeem with Your Points", () -> {} //Add request for redemption, redemption processing too.
        ),
        TIER_PROGRESSION("Manage Your Tier Progression", () ->{} //Add tier upgrade
        );
        private final String label;
        private final Runnable action;
        MenuOption(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }
        @Override
        public String label() {
            return label;
        }
        @Override
        public void run() {
            action.run();
        }
    }
    public void run() {
        for (;;) {
            LoyaltyUI.MenuOption selected = Menu.prompt(TITLE, LoyaltyUI.MenuOption.values());

            if (selected == LoyaltyUI.MenuOption.BACK) return;
            selected.run();
        }
    }
}
