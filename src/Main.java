import boundary.AllocationUI;
import boundary.HousekeepingUI;
import boundary.FrontDeskUI;
import boundary.LoyaltyUI;
import control.AllocationControl;
import control.BookingControl;
import control.HousekeepingControl;
import control.LoyaltyControl;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;
import utility.OutputHelper;

/**
 * @author Lam Yong Zhe
 */
public class Main {
    private static final String TITLE = "TARUMT Resorts";
    private static final String BANNER = "(^_^)/ Welcome!";

    // Wired once, here. Each control loads its own module's DAO and is then the
    // single way in for everybody else, so no two modules hold their own copy of
    // the same data — and a screen's state survives being left and re-entered.
    private static final HousekeepingControl HOUSEKEEPING_CONTROL = new HousekeepingControl();
    private static final LoyaltyControl LOYALTY_CONTROL = new LoyaltyControl();
    private static final BookingControl BOOKINGS = new BookingControl(HOUSEKEEPING_CONTROL);
    private static final AllocationControl ALLOCATION_CONTROL =
            new AllocationControl(BOOKINGS, HOUSEKEEPING_CONTROL);

    static {
        // M2 reads bookings from M4, and cancelling a booking has to pull its
        // request back out of M2's queue. One of the two directions cannot be a
        // constructor argument, and this is it.
        BOOKINGS.attachAllocation(ALLOCATION_CONTROL);
    }

    private static final HousekeepingUI HOUSEKEEPING_UI = new HousekeepingUI(HOUSEKEEPING_CONTROL);
    private static final AllocationUI ALLOCATION_UI = new AllocationUI(ALLOCATION_CONTROL);
    private static final FrontDeskUI FRONT_DESK_UI = new FrontDeskUI(BOOKINGS);
    private static final LoyaltyUI LOYALTY_UI = new LoyaltyUI(LOYALTY_CONTROL);

    private enum MenuOption implements MenuItem {
        EXIT("Exit",
                () -> {}
        ),
        VIP_PRIORITY("VIP & Loyalty Tier-Priority Room Allocation",
                ALLOCATION_UI::run
        ),
        HOUSEKEEPING("Housekeeping and Task Log",
                HOUSEKEEPING_UI::run
        ),
        FRONT_DESK("Front-Desk Service",
                FRONT_DESK_UI::run
        ),
        LOYALTY("Loyalty and Rewards Service",
                LOYALTY_UI::run
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
            OutputHelper.clearScreen();
            MenuOption selected = Menu.prompt(TITLE, BANNER, MenuOption.values());

            if (selected == MenuOption.EXIT) {
                return;
            }
            selected.run();
            InputHelper.waitForEnter();
        }
    }
}
