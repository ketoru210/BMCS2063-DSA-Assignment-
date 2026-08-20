package boundary;

import control.BookingControl;
import control.BookingControl.SortKey;
import entity.Booking;
import entity.RoomType;
import java.time.format.DateTimeFormatter;
import utility.InputHelper;
import utility.MenuItem;
import utility.OutputHelper;
import utility.TableRenderer;
import utility.TableRenderer.Align;
import utility.TreeRenderer;

/**
 * Boundary: CLI for Front-Desk Service (Module 4).
 * <p>
 * The tree is drawn upright and whole, keys only. Nodes are placed by in-order
 * position, so reading a row left to right is the in-order traversal, every
 * parent sits between its own subtrees, and the width follows the number of
 * bookings instead of doubling with the height.
 *
 * @author Fong Qin Wen
 */
public class FrontDeskUI {

    private static final String TITLE = "Front-Desk Service";
    private static final int TREE_GAP = 2;
    private static final DateTimeFormatter STAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final BookingControl control;

    private String notice;

    public FrontDeskUI(BookingControl control) {
        this.control = control;
    }

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        NEW_BOOKING("New Walk-in Booking"),
        LOOK_UP("Look Up Booking"),
        LIST("View All Bookings"),
        CHECK_IN("Check-in Guest"),
        CHECK_OUT("Check-out Guest"),
        CANCEL("Cancel Booking"),
        PURGE("Purge Cancelled Bookings"),
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
                case LIST:
                    listAll();
                    break;
                case CHECK_IN:
                    checkIn();
                    break;
                case CHECK_OUT:
                    checkOut();
                    break;
                case CANCEL:
                    cancel();
                    break;
                case PURGE:
                    purge();
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

        String[] slots = new String[shape.length];
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] != null) {
                slots[i] = shape[i].getConfirmationNo();
            }
        }

        System.out.println("Bookings by confirmation no. ("
                + control.size() + " on file, height " + control.getTreeHeight() + ")\n");
        String[] rows = TreeRenderer.renderCompact(slots, TREE_GAP);
        for (int i = 0; i < rows.length; i++) {
            System.out.println(rows[i]);
        }
        System.out.println();
    }

    private void newWalkIn() {
        for (;;) {
            String name = InputHelper.readLine("\nCustomer name (0 = back) > ");
            if (name.equals("0")) {
                return;
            }
            if (name.isEmpty()) {
                OutputHelper.printErr("Customer name is required.");
                continue;
            }

            RoomType roomType = readRoomType();
            if (roomType == null) {
                // a sub-step back only restarts the booking, it does not leave
                continue;
            }

            int nights = InputHelper.readInt("Nights > ");
            Booking booking = control.createWalkIn(name, roomType, nights);
            if (booking == null) {
                OutputHelper.printErr("Nights must be at least 1.");
                continue;
            }
            notice = "Booked. Confirmation no. " + booking.getConfirmationNo()
                    + " (" + money(control.revenueOf(booking)) + ")";
            OutputHelper.printOK(notice);
        }
    }

    private RoomType readRoomType() {
        RoomType[] types = RoomType.values();
        System.out.println();
        String[][] cells = new String[types.length][];
        for (int i = 0; i < types.length; i++) {
            cells[i] = new String[]{
                String.valueOf(i + 1),
                String.valueOf(types[i]),
                money(types[i].getRatePerNight()) + " / night"
            };
        }
        String[] menu = TableRenderer.renderBordered(new String[]{"No.", "Room Type", "Rate"},
                cells, new Align[]{Align.LEFT, Align.LEFT, Align.RIGHT});
        for (int i = 0; i < menu.length; i++) {
            System.out.println(menu[i]);
        }
        System.out.println("[0] Back");
        for (;;) {
            int choice = InputHelper.readInt("Room type > ");
            if (choice == 0) {
                return null;
            }
            if (choice >= 1 && choice <= types.length) {
                return types[choice - 1];
            }
            OutputHelper.printErr("Please enter a number between 0 and " + types.length + ".");
        }
    }

    private void lookUp() {
        for (;;) {
            String confirmationNo = InputHelper.readLine("\nConfirmation no. (0 = back) > ");
            if (confirmationNo.equals("0")) {
                return;
            }
            Booking found = control.findByConfirmationNo(confirmationNo);

            System.out.println();
            if (found != null) {
                OutputHelper.printOK("Found in " + control.getTreeHeight()
                        + " comparisons at most:");
                printBookings(null, new Booking[]{found});
            } else {
                OutputHelper.printErr("No booking with confirmation no. " + confirmationNo);
                Booking below = control.nearestBelow(confirmationNo);
                Booking above = control.nearestAbove(confirmationNo);
                if (below == null && above == null) {
                    System.out.println("  Nothing on file to compare against.");
                } else {
                    System.out.println("Closest on file:");
                    int near = (below == null ? 0 : 1) + (above == null ? 0 : 1);
                    String[] sides = new String[near];
                    Booking[] rows = new Booking[near];
                    int at = 0;
                    if (below != null) {
                        sides[at] = "below";
                        rows[at++] = below;
                    }
                    if (above != null) {
                        sides[at] = "above";
                        rows[at++] = above;
                    }
                    printBookings(sides, rows);
                }
            }
            InputHelper.waitForEnter();
        }
    }

    /** Every booking on file, which the desk needs far more often than a report. */
    private void listAll() {
        Booking[] all = control.getAllBookings();
        System.out.println();
        if (all.length == 0) {
            OutputHelper.printErr("No bookings on file.");
            InputHelper.waitForEnter();
            return;
        }

        // the order is not sorted here: an in-order walk of the tree already
        // hands them over ascending by the key it is built on
        OutputHelper.printBlue(all.length + " booking" + (all.length == 1 ? "" : "s")
                + " on file, in the tree's in-order walk (ascending confirmation no.)");
        System.out.println();
        printBookings(null, all);
        InputHelper.waitForEnter();
    }

    /**
     * One row per booking under a shared set of headings. A non-null sides array
     * adds a leading column, which is how the near-miss rows say which side of
     * the missing key they sit on.
     */
    private void printBookings(String[] sides, Booking[] rows) {
        boolean labelled = sides != null;
        String[] headers = {"", "Conf No.", "Customer", "Type", "Room", "Stay",
            "Nights", "Status", "Charge"};
        Align[] aligns = {Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT,
            Align.LEFT, Align.RIGHT, Align.LEFT, Align.RIGHT};
        int skip = labelled ? 0 : 1;

        String[][] cells = new String[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            Booking booking = rows[i];
            String[] row = {
                labelled ? sides[i] : "",
                booking.getConfirmationNo(),
                booking.getMember().getName(),
                String.valueOf(booking.getRoomType()),
                booking.isAllocated() ? booking.getRoom().getRoomNo() : "-",
                booking.getCheckIn().format(STAY_DATE)
                        + " - " + booking.getCheckOut().format(STAY_DATE),
                String.valueOf(booking.getNights()),
                booking.getStatus(),
                // a cancelled booking is still shown, but earns nothing
                control.earnsRevenue(booking) ? money(control.revenueOf(booking)) : "-"
            };
            cells[i] = new String[row.length - skip];
            System.arraycopy(row, skip, cells[i], 0, cells[i].length);
        }

        String[] used = new String[headers.length - skip];
        Align[] usedAligns = new Align[aligns.length - skip];
        System.arraycopy(headers, skip, used, 0, used.length);
        System.arraycopy(aligns, skip, usedAligns, 0, usedAligns.length);

        String[] table = TableRenderer.renderBordered(used, cells, usedAligns);
        for (int i = 0; i < table.length; i++) {
            System.out.println(table[i]);
        }
    }

    private void checkIn() {
        for (;;) {
            String confirmationNo = InputHelper.readLine("\nConfirmation no. (0 = back) > ");
            if (confirmationNo.equals("0")) {
                return;
            }
            if (control.checkIn(confirmationNo)) {
                notice = "Checked in " + confirmationNo + ".";
                OutputHelper.printOK(notice);
                continue;
            }

            Booking found = control.findByConfirmationNo(confirmationNo);
            OutputHelper.printErr(found == null
                    ? "No booking with confirmation no. " + confirmationNo
                    : found.isAllocated()
                            ? "Cannot check in: status is " + found.getStatus() + "."
                            : "Cannot check in: status is " + found.getStatus()
                                    + " and no room has been allocated.");
        }
    }

    private void checkOut() {
        for (;;) {
            String confirmationNo = InputHelper.readLine("\nConfirmation no. (0 = back) > ");
            if (confirmationNo.equals("0")) {
                return;
            }
            Booking found = control.findByConfirmationNo(confirmationNo);
            // read while the booking still holds it — checking out lets the room go
            String roomNo = found != null && found.isAllocated()
                    ? found.getRoom().getRoomNo() : "-";

            if (control.checkOut(confirmationNo)) {
                notice = "Checked out " + confirmationNo + ". Room " + roomNo
                        + " is back in the pool and now Dirty for housekeeping.";
                OutputHelper.printOK(notice);
                continue;
            }
            OutputHelper.printErr(found == null
                    ? "No booking with confirmation no. " + confirmationNo
                    : "Cannot check out: status is " + found.getStatus() + ", not Checked-in.");
        }
    }

    /**
     * A wrong confirmation number asks again rather than dropping back to the menu:
     * the desk is reading it off a guest, and a typo should not cost the screen.
     */
    private void cancel() {
        for (;;) {
            String confirmationNo = InputHelper.readLine("\nConfirmation no. to cancel (0 = back) > ");
            if (confirmationNo.equals("0")) {
                return;
            }
            if (confirmationNo.isEmpty()) {
                OutputHelper.printErr("A confirmation number is required.");
                continue;
            }

            Booking found = control.findByConfirmationNo(confirmationNo);
            if (found == null) {
                OutputHelper.printErr("No booking with confirmation no. " + confirmationNo);
                continue;
            }
            if (!control.isCancellable(found)) {
                OutputHelper.printErr("Already " + found.getStatus() + " - nothing to cancel.");
                continue;
            }

            System.out.println();
            printBookings(null, new Booking[]{found});
            String answer = InputHelper.readLine("Cancel this booking? [y/N] > ");
            if (!answer.equalsIgnoreCase("y")) {
                notice = "Cancellation aborted.";
                return;
            }

            Booking cancelled = control.cancel(confirmationNo);
            notice = cancelled == null
                    ? "Nothing was cancelled."
                    : "Cancelled " + cancelled.getConfirmationNo() + ". It stays on file as "
                            + cancelled.getStatus() + ", and any room or queue place it held is released.";
            return;
        }
    }

    /** The only path that takes a node out of the tree, so the height can move. */
    private void purge() {
        int heightBefore = control.getTreeHeight();
        int sizeBefore = control.size();

        int removed = control.purgeCancelled();
        notice = removed == 0
                ? "No cancelled booking is on file."
                : "Purged " + removed + " cancelled booking" + (removed == 1 ? "" : "s")
                        + ". On file " + sizeBefore + " -> " + control.size()
                        + ", tree height " + heightBefore + " -> " + control.getTreeHeight() + ".";
    }

    private void treeHealthReport() {
        OutputHelper.clearScreen();
        OutputHelper.printReportHeader("Tree Health Report", "every booking on file");
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

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Records : %d    Height : %d of %d optimal    Degeneracy : %.1f%%",
                size, height, optimal, degeneracy));
        InputHelper.waitForEnter();
    }

    private void rangeAudit() {
        OutputHelper.printTitle("Confirmation Range Audit");

        String low = InputHelper.readLine("\nFrom confirmation no. > ");
        String high = InputHelper.readLine("To confirmation no.   > ");

        Booking[] batch = control.findInRange(low, high);
        if (batch.length == 0) {
            System.out.println();
            OutputHelper.printErr("No bookings in that range.");
            InputHelper.waitForEnter();
            return;
        }

        SortKey key = askSortKey();
        if (key == null) {
            return;
        }
        boolean descending = askDescending(key);
        batch = control.sortBy(batch, key, descending);

        System.out.println();
        OutputHelper.printReportHeader("Confirmation Range Audit",
                "confirmation no. " + low + " to " + high
                        + ", ordered by " + key + (descending ? " (high to low)" : " (low to high)"));
        System.out.println();

        String[] headers = {"CONF NO", "NAME", "TYPE", "NIGHTS", "STATUS", "CHARGE"};
        Align[] aligns = {Align.LEFT, Align.LEFT, Align.LEFT, Align.RIGHT, Align.LEFT, Align.RIGHT};
        String[][] cells = new String[batch.length][];
        for (int i = 0; i < batch.length; i++) {
            // a cancelled row is still listed, but shows no money against it
            boolean earns = control.earnsRevenue(batch[i]);
            cells[i] = new String[]{
                batch[i].getConfirmationNo(),
                batch[i].getMember().getName(),
                String.valueOf(batch[i].getRoomType()),
                String.valueOf(batch[i].getNights()),
                batch[i].getStatus(),
                earns ? money(control.revenueOf(batch[i])) : "-"
            };
        }
        String[] rows = TableRenderer.render(headers, cells, aligns);
        for (int i = 0; i < rows.length; i++) {
            System.out.println("  " + rows[i]);
        }

        double total = control.totalRevenueOf(batch);
        int earning = control.countEarning(batch);
        System.out.println();
        System.out.printf("  Matched      : %d of %d on file  (%d earning, %d cancelled)%n",
                batch.length, control.size(), earning, batch.length - earning);
        System.out.printf("  Total charge : %s%n", money(total));
        System.out.printf("  Average      : %s per booking, %.1f nights%n",
                money(earning == 0 ? 0.0 : total / earning), control.averageNightOf(batch));
        System.out.println();

        RoomType[] types = RoomType.values();
        double[] revenue = control.revenueByRoomType(batch);
        String[] shareHeaders = {"ROOM TYPE", "CHARGE", "SHARE", ""};
        Align[] shareAligns = {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.LEFT};
        String[][] shareCells = new String[types.length][];
        for (int i = 0; i < types.length; i++) {
            double share = total == 0.0 ? 0.0 : revenue[i] * 100.0 / total;
            shareCells[i] = new String[]{
                String.valueOf(types[i]),
                money(revenue[i]),
                String.format("%.1f%%", share),
                bar(share)
            };
        }
        System.out.println("  Charge by room type");
        String[] shareRows = TableRenderer.render(shareHeaders, shareCells, shareAligns);
        for (int i = 1; i < shareRows.length; i++) {
            System.out.println("    " + shareRows[i]);
        }

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Records : %d of %d on file    Total charge : %s    Average : %s",
                batch.length, control.size(), money(total),
                money(earning == 0 ? 0.0 : total / earning)));
        InputHelper.waitForEnter();
    }

    /** Null means the user backed out. */
    private SortKey askSortKey() {
        SortKey[] keys = SortKey.values();
        System.out.println("\nOrder the audit by:");
        System.out.println("[0] Back");
        for (int i = 0; i < keys.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, keys[i]);
        }
        for (;;) {
            int pick = InputHelper.readInt("\nPlease Select > ");
            if (pick == 0) {
                return null;
            }
            if (pick >= 1 && pick <= keys.length) {
                return keys[pick - 1];
            }
            OutputHelper.printErr("Please enter a number between 0 and " + keys.length + ".");
        }
    }

    private boolean askDescending(SortKey key) {
        System.out.println("\n" + key + " order:");
        System.out.println("[1] Low to high");
        System.out.println("[2] High to low");
        for (;;) {
            int pick = InputHelper.readInt("\nPlease Select > ");
            if (pick == 1 || pick == 2) {
                return pick == 2;
            }
            OutputHelper.printErr("Please enter 1 or 2.");
        }
    }

    private String bar(double percent) {
        return "#".repeat((int) Math.round(percent / 5.0));
    }

    private String money(double amount) {
        return String.format("RM %,.2f", amount);
    }
}