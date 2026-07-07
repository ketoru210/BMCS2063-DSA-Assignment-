import boundary.AllocationUI;
import boundary.HousekeepingUI;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;

public class Main {
    private static final String TITLE = "TARUMT Resorts";
    private static final String BANNER = "(^_^)/ Welcome!";

    private enum MenuOption implements MenuItem {
        EXIT("Exit",
                () -> {}
        ),
        WALK_IN("Walk-In Registration & Standard Booking Procedure",
                () -> System.out.println("//TODO: Redirect to Module 1")
        ),
        VIP_PRIORITY("VIP & Loyalty Tier-Priority Room Allocation",
                () -> new AllocationUI().run()
        ),
        HOUSEKEEPING("Housekeeping and Task Log",
                () -> new HousekeepingUI().run()
        ),
        FRONT_DESK("Front-Desk Service",
                () -> System.out.println("//TODO: Redirect to Module 4")
        ),
        LOYALTY("Loyalty and Rewards Service",
                () -> System.out.println("//TODO: Redirect to Module 5")
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

    public static void main(String[] args) {
        for (;;) {
            MenuOption selected = Menu.prompt(TITLE, BANNER, MenuOption.values());

            if (selected == MenuOption.EXIT) {
                return;
            }
            selected.run();
            InputHelper.waitForEnter();
        }
    }
}
