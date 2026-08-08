import boundary.AllocationUI;
import boundary.HousekeepingUI;
import control.AllocationControl;
import control.BookingControl;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;

public class Main {
    private static final String TITLE = "TARUMT Resorts";
    private static final String BANNER = "(^_^)/ Welcome!";

    // Wired once, here. Each control loads its own module's DAO and is then the
    // single way in for everybody else, so no two modules hold their own copy of
    // the same data — and a screen's state survives being left and re-entered.
    private static final BookingControl BOOKINGS = new BookingControl();

    private static final AllocationUI ALLOCATION_UI =
            new AllocationUI(new AllocationControl(BOOKINGS));
    private static final HousekeepingUI HOUSEKEEPING_UI = new HousekeepingUI();

    private enum MenuOption implements MenuItem {
        EXIT("Exit",
                () -> {}
        ),
        VIP_PRIORITY("VIP & Loyalty Tier-Priority Room Allocation",
                () -> ALLOCATION_UI.run()
        ),
        HOUSEKEEPING("Housekeeping and Task Log",
                () -> HOUSEKEEPING_UI.run()
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
