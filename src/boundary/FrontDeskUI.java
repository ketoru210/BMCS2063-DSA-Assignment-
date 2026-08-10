package boundary;

import control.BookingControl;
import entity.Booking;
import entity.RoomType;
import utility.InputHelper;
import utility.MenuItem;
import utility.OutputHelper;

/**
 * Boundary: CLI for Front-Desk Service (Module 4).
 * <p>
 * The tree is drawn sideways with the largest confirmation number at the top, so
 * reading the screen downwards is the in-order traversal and the shape of the
 * tree is visible at the same time. Drawing it upright would need the full width
 * of eight-digit keys at every level, which does not fit a console.
 *
 * @author QW
 */
public class FrontDeskUI {

    private static final String TITLE = "Front-Desk Service";
    private static final String INDENT = "      ";

    private final BookingControl control;

    private String notice;

    public FrontDeskUI(BookingControl control) {
        this.control = control;
    }

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        NEW_BOOKING("New Walk-in Booking"),
        LOOK_UP("Look Up Booking"),
        CHECK_IN("Check-in Guest"),
        CANCEL("Cancel Booking"),
        HEALTH("Tree Health Report"),
        RANGE("Confirmation Range Audit");

        private final String label;

        MenuOption(String label) {
            this.label = label;
        }

        @Override
        public String label() {
            return label;
        }

        @Override
        public void run() {
            // execution handled by the switch in run()
        }
    }

    public void run() {
        for (;;) {
            MenuOption selected = render();

            switch (selected) {
                case BACK:
                    return;
                case NEW_BOOKING:
                    newWalkIn();
                    break;
                case LOOK_UP:
                    lookUp();
                    break;
                case CHECK_IN:
                    checkIn();
                    break;
                case CANCEL:
                    cancel();
                    break;
                case HEALTH:
                    treeHealthReport();
                    break;
                case RANGE:
                    rangeAudit();
                    break;
            }
        }
    }

    private MenuOption render() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle(TITLE);
        System.out.println();

        if (notice != null) {
            OutputHelper.printOK(notice + "\n");
            notice = null;
        }

        drawTree();

        MenuOption[] options = MenuOption.values();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].label();
        }
        OutputHelper.printOptions(labels);

        for (;;) {
            int choice = InputHelper.readInt("\nPlease Select > ");
            if (choice >= 0 && choice < options.length) {
                return options[choice];
            }
            OutputHelper.printErr("Please enter number between 0 and " + (options.length - 1) + " (inclusive)");
        }
    }

    private void drawTree() {
        Booking[] shape = control.getTreeShape();
        if (shape.length == 0) {
            OutputHelper.printBlue("(no bookings on file)\n");
            return;
        }
        System.out.println("Bookings by confirmation no. (highest at top, "
                + control.size() + " on file, height " + control.getTreeHeight() + ")\n");
        drawSlot(shape, 0, 0);
        System.out.println();
    }

    private void drawSlot(Booking[] shape, int index, int depth) {
        if (index >= shape.length || shape[index] == null) {
            return;
        }
        drawSlot(shape, 2 * index + 2, depth + 1);
        System.out.println(INDENT.repeat(depth) + shape[index].getConfirmationNo()
                + "  " + shape[index].getMember().getName());
        drawSlot(shape, 2 * index + 1, depth + 1);
    }

    private void newWalkIn() {
        String name = InputHelper.readLine("\nGuest name > ");
        if (name.isEmpty()) {
            OutputHelper.printErr("Guest name is required.");
            InputHelper.waitForEnter();
            return;
        }

        RoomType roomType = readRoomType();
        if (roomType == null) {
            return;
        }

        int nights = InputHelper.readInt("Nights > ");
        Booking booking = control.createWalkIn(name, roomType, nights);
        if (booking == null) {
            OutputHelper.printErr("Nights must be at least 1.");
            InputHelper.waitForEnter();
            return;
        }
        notice = "Booked. Confirmation no. " + booking.getConfirmationNo()
                + " (" + money(control.revenueOf(booking)) + ")";
    }

    private RoomType readRoomType() {
        RoomType[] types = RoomType.values();
        System.out.println();
        for (int i = 0; i < types.length; i++) {
            System.out.println("[" + (i + 1) + "] " + types[i] + " - " + money(types[i].getRatePerNight()) + " / night");
        }
        int choice = InputHelper.readInt("Room type > ");
        if (choice < 1 || choice > types.length) {
            OutputHelper.printErr("No such room type.");
            InputHelper.waitForEnter();
            return null;
        }
        return types[choice - 1];
    }

    private void lookUp() {
        String confirmationNo = InputHelper.readLine("\nConfirmation no. > ");
        Booking found = control.findByConfirmationNo(confirmationNo);

        System.out.println();
        if (found != null) {
            OutputHelper.printOK("Found in " + control.getTreeHeight() + " comparisons at most:");
            System.out.println("  " + found);
            System.out.println("  Charge: " + money(control.revenueOf(found))
                    + " (" + found.getNights() + " nights)");
        } else {
            OutputHelper.printErr("No booking with confirmation no. " + confirmationNo);
            Booking below = control.nearestBelow(confirmationNo);
            Booking above = control.nearestAbove(confirmationNo);
            if (below == null && above == null) {
                System.out.println("  Nothing on file to compare against.");
            } else {
                System.out.println("  Closest on file:");
                if (below != null) {
                    System.out.println("    below > " + below);
                }
                if (above != null) {
                    System.out.println("    above > " + above);
                }
            }
        }
        InputHelper.waitForEnter();
    }

    private void checkIn() {
        String confirmationNo = InputHelper.readLine("\nConfirmation no. > ");
        if (control.checkIn(confirmationNo)) {
            notice = "Checked in " + confirmationNo + ".";
            return;
        }
        Booking found = control.findByConfirmationNo(confirmationNo);
        OutputHelper.printErr(found == null
                ? "\nNo booking with confirmation no. " + confirmationNo
                : "\nCannot check in: status is already " + found.getStatus() + ".");
        InputHelper.waitForEnter();
    }

    private void cancel() {
        String confirmationNo = InputHelper.readLine("\nConfirmation no. > ");
        Booking found = control.findByConfirmationNo(confirmationNo);
        if (found == null) {
            OutputHelper.printErr("\nNo booking with confirmation no. " + confirmationNo);
            InputHelper.waitForEnter();
            return;
        }

        System.out.println("\n  " + found);
        String answer = InputHelper.readLine("Cancel this booking? [y/N] > ");
        if (!answer.equalsIgnoreCase("y")) {
            notice = "Cancellation aborted.";
            return;
        }

        int heightBefore = control.getTreeHeight();
        Booking removed = control.cancel(confirmationNo);
        notice = removed == null
                ? "Nothing was removed."
                : "Cancelled " + removed.getConfirmationNo()
                        + ". Tree height " + heightBefore + " -> " + control.getTreeHeight() + ".";
    }

    private void treeHealthReport() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("Tree Health Report");
        System.out.println();

        int size = control.size();
        if (size == 0) {
            OutputHelper.printBlue("No bookings on file.");
            InputHelper.waitForEnter();
            return;
        }

        int height = control.getTreeHeight();
        int optimal = control.getOptimalHeight();
        double degeneracy = control.getDegeneracyPercent();

        System.out.printf("  Bookings on file        : %d%n", size);
        System.out.printf("  Current height          : %d%n", height);
        System.out.printf("  Perfectly balanced      : %d%n", optimal);
        System.out.printf("  Worst case (linked list): %d%n", size);
        System.out.printf("  Degeneracy              : %.1f%%%n", degeneracy);
        System.out.printf("  Lookup comparisons      : %d worst case, %d if balanced%n", height, optimal);
        System.out.printf("  Confirmation no. rerolls: %d%n", control.getRerolls());
        System.out.println();
        System.out.println("  Lowest  > " + control.getLowestConfirmationNo());
        System.out.println("  Highest > " + control.getHighestConfirmationNo());
        System.out.println();

        if (degeneracy < 34.0) {
            OutputHelper.printOK("  Random confirmation numbers are keeping the tree shallow.");
        } else if (degeneracy < 67.0) {
            OutputHelper.printBlue("  Leaning to one side. Lookups cost more than they need to.");
        } else {
            OutputHelper.printErr("  Close to a linked list. Lookups are degrading towards O(n).");
        }
        InputHelper.waitForEnter();
    }

    private void rangeAudit() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("Confirmation Range Audit");

        String low = InputHelper.readLine("\nFrom confirmation no. > ");
        String high = InputHelper.readLine("To confirmation no.   > ");

        Booking[] batch = control.findInRange(low, high);
        System.out.println();
        if (batch.length == 0) {
            OutputHelper.printErr("No bookings in that range.");
            InputHelper.waitForEnter();
            return;
        }

        System.out.printf("  %-12s %-18s %-8s %-6s %s%n",
                "CONF NO", "GUEST", "TYPE", "NIGHTS", "CHARGE");
        for (int i = 0; i < batch.length; i++) {
            System.out.printf("  %-12s %-18s %-8s %-6d %s%n",
                    batch[i].getConfirmationNo(),
                    batch[i].getMember().getName(),
                    batch[i].getRoomType(),
                    batch[i].getNights(),
                    money(control.revenueOf(batch[i])));
        }

        double total = control.totalRevenueOf(batch);
        System.out.println();
        System.out.printf("  Matched      : %d of %d on file%n", batch.length, control.size());
        System.out.printf("  Total charge : %s%n", money(total));
        System.out.printf("  Average      : %s per booking, %.1f nights%n",
                money(total / batch.length), control.averageNightOf(batch));
        System.out.println();

        RoomType[] types = RoomType.values();
        double[] revenue = control.revenueByRoomType(batch);
        System.out.println("  Charge by room type");
        for (int i = 0; i < types.length; i++) {
            double share = total == 0.0 ? 0.0 : revenue[i] * 100.0 / total;
            System.out.printf("    %-8s %12s  %5.1f%%  %s%n",
                    types[i], money(revenue[i]), share, bar(share));
        }
        InputHelper.waitForEnter();
    }

    private String bar(double percent) {
        return "#".repeat((int) Math.round(percent / 5.0));
    }

    private String money(double amount) {
        return String.format("RM %,.2f", amount);
    }
}