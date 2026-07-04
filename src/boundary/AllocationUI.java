package boundary;

import utility.Menu;
import utility.MenuItem;

/**
 * Boundary: CLI for VIP & Loyalty Tier-Priority Room Allocation
 * (Module 2 - Heap).
 */
public class AllocationUI {
    private static final String TITLE = "VIP & Loyalty Tier-Priority Room Allocation";

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu",
                () -> {}
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
            MenuOption selected = Menu.prompt(TITLE, MenuOption.values());

            if (selected == MenuOption.BACK) return;
            selected.run();
        }
    }
    // TODO: implement allocation menu and user interaction
}
