package boundary;

import control.AllocationControl;
import entity.Allocation;
import entity.Booking;
import entity.SpecialCategory;
import utility.InputHelper;
import utility.MenuItem;
import utility.OutputHelper;

/**
 * Boundary: CLI for VIP &amp; Loyalty Tier-Priority Room Allocation (Module 2).
 * <p>
 * The queue is drawn as the binary tree the heap actually is, and it is on
 * screen at all times — the menu sits underneath it. Nodes carry
 * {@code #entry(serve order)} rather than the sort key: the key alone would put
 * a special-category entry with a low score above a normal entry with a high
 * one, which reads as a broken heap even though it is correct. Serve order is
 * monotone across every layer of the comparison, so the parent's number is
 * always the smaller one and nothing looks out of place.
 * <p>
 * This screen builds its own menu instead of calling {@code Menu.prompt} so that
 * individual lines of the view can be colored; the layout convention is the same.
 *
 * @author YZ
 */
public class AllocationUI {

    private boolean firstTime = true;
    private static final String TITLE = "VIP & Loyalty Tier-Priority Room Allocation";
    private static final int CELL = 8;
    private static final String DEAD_SCORE = "N/A";

    private final AllocationControl control;

    /** Shown once at the top of the next redraw, so success needs no [Enter]. */
    private String notice;

    /** Handed the control rather than building one: the queue and the simulated
     *  clock have to survive leaving this screen and coming back. */
    public AllocationUI(AllocationControl control) {
        this.control = control;
    }

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        FAST_FORWARD("Fast-Forward Clock"),
        SERVE("Serve Next Request"),
        ADD("Add Request to Queue"),
        CANCEL("Cancel a Request");

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
                case FAST_FORWARD:
                    fastForwardClock();
                    break;
                case SERVE:
                    serveNext();
                    break;
                case ADD:
                    addRequest();
                    break;
                case CANCEL:
                    cancelRequest();
                    break;
            }
        }
    }

    // --- screen ---

    /** Redraws the queue with the menu underneath, and returns a valid choice. */
    private MenuOption render() {
        MenuOption[] options = MenuOption.values();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].label();
        }

        for (;;) {
            if (!firstTime) System.out.println("\n\n");
            else {
                OutputHelper.clearScreen();
                firstTime = false;
            }
            OutputHelper.printTitle(TITLE);
            System.out.println();

            printQueue();
            System.out.println();
            OutputHelper.printOptions(labels);

            int choice = InputHelper.readInt("\nPlease Select > ");
            if (choice >= 0 && choice < options.length) {
                return options[choice];
            }
            fail("Please enter a number between 0 and " + (options.length - 1) + ".");
        }
    }

    private void printQueue() {
        if (notice != null) {
            OutputHelper.printOK(notice);
            System.out.println();
            notice = null;
        }

        OutputHelper.printBlue("now = " + control.getClockMinute() + " min (simulated)");
        System.out.println();

        if (control.isEmpty()) {
            OutputHelper.printBlue("Nobody is waiting.");
            return;
        }

        Allocation[] levelOrder = control.getLevelOrder();
        Allocation[] serveOrder = control.getServeOrder();

        printTree(levelOrder, serveOrder);
        System.out.println();
        printTable(serveOrder);
    }

    /**
     * Draws the heap array as a centred binary tree. Level order is already the
     * array order, so no traversal and no node objects are needed — but the
     * branch rows need both layers' centres at once, so every label is measured
     * up front. The root is highlighted because it is always served next.
     */
    private void printTree(Allocation[] levelOrder, Allocation[] serveOrder) {
        int layers = layersFor(levelOrder.length);

        String[][] labels = new String[layers][];
        int[][] centres = new int[layers][];

        // calculate the needed value first (where is the node, how many node, how long the width)
        for (int depth = 0; depth < layers; depth++) {
            // (1 << n) means (1 * 2 ^ n)
            // - use this instead if Math.pow() is because i want result value in integer type
            int first = (1 << depth) - 1;  // index in array of first node of the current layer
            int count = Math.min(1 << depth, levelOrder.length - first);  // number of node of the current layer
            int block = CELL * (1 << (layers - 1 - depth));  // width of node

            labels[depth] = new String[count];
            centres[depth] = new int[count];
            for (int k = 0; k < count; k++) {
                String label = labelOf(levelOrder[first + k], serveOrder);
                int column = k * block + (block - label.length()) / 2;
                labels[depth][k] = label;
                centres[depth][k] = column + (label.length() - 1) / 2;
            }
        }

        for (int depth = 0; depth < layers; depth++) {
            String row = rowOf(labels[depth], centres[depth]);
            if (depth == 0) {
                OutputHelper.printOK(row);
            } else {
                System.out.println(row);
            }
            if (depth + 1 < layers) {
                System.out.println(branchRow(centres[depth + 1]));
            }
        }
        System.out.println();
        OutputHelper.printBlue("  #n = entry no. (permanent)    (n) = serve order");
    }

    private String rowOf(String[] labels, int[] centres) {
        StringBuilder line = new StringBuilder();
        for (int k = 0; k < labels.length; k++) {
            int start = centres[k] - (labels[k].length() - 1) / 2;
            while (line.length() < start) {
                line.append(' ');
            }
            line.append(labels[k]);
        }
        return line.toString();
    }

    /**
     * Brackets each sibling pair. A single slash cannot span the gap — the
     * horizontal distance to a child is a quarter of the parent's block, eight
     * columns at the root — so the dashes carry the distance and the slashes
     * only mark which end is whose.
     */
    private String branchRow(int[] childCentres) {
        StringBuilder line = new StringBuilder();
        for (int k = 0; k < childCentres.length; k += 2) {
            while (line.length() < childCentres[k]) {
                line.append(' ');
            }
            line.append('/');

            if (k + 1 < childCentres.length) {
                while (line.length() < childCentres[k + 1]) {
                    line.append('-');
                }
                line.append('\\');
            }
        }
        return line.toString();
    }

    private void printTable(Allocation[] serveOrder) {
        OutputHelper.printBlue(String.format("%2s  %-6s %-17s %-9s %-13s %7s %6s %7s",
                "#", "Entry", "Guest", "Tier", "Special", "Waited", "P(0)", "P(now)"));

        for (int i = 0; i < serveOrder.length; i++) {
            Allocation entry = serveOrder[i];
            boolean scored = entry.getCategory() == SpecialCategory.NONE;

            String row = String.format("%2d  #%-5d %-17s %-9s %-13s %3d min %6s %7s",
                    i + 1,
                    entry.getEntryNo(),
                    entry.getBooking().getMember().getName(),
                    entry.getTier(),
                    entry.getCategory(),
                    control.getClockMinute() - entry.getArrivalMinute(),
                    scored ? String.valueOf(entry.getInvariantPriority()) : DEAD_SCORE,
                    scored ? String.valueOf(control.livePriority(entry)) : DEAD_SCORE);

            // the first row is the root of the tree above — same entry, same color
            if (i == 0) {
                OutputHelper.printOK(row);
            } else {
                System.out.println(row);
            }
        }
    }

    /** Smallest L with 2^L - 1 >= n. Integer loop, so no floating-point rounding. */
    private int layersFor(int numOfEntries) {
        int layers = 0;
        while (((1 << layers) - 1) < numOfEntries) {
            layers++;
        }
        return layers;
    }

    private String labelOf(Allocation entry, Allocation[] serveOrder) {
        return "#" + entry.getEntryNo() + "(" + rankOf(entry, serveOrder) + ")";
    }

    private int rankOf(Allocation entry, Allocation[] serveOrder) {
        for (int i = 0; i < serveOrder.length; i++) {
            if (serveOrder[i].getEntryNo() == entry.getEntryNo()) {
                return i + 1;
            }
        }
        return 0;
    }

    // --- actions ---

    private void fastForwardClock() {
        int minutes = InputHelper.readInt("\nFast-forward by how many minutes (0 to go back) > ");
        if (minutes == 0) {
            return;
        }
        if (minutes < 0) {
            fail("Minutes must be positive.");
            return;
        }
        control.fastForward(minutes);
        notice = "Clock is now at " + control.getClockMinute() + " min.";
    }

    private void serveNext() {
        Allocation served = control.serveNext();
        if (served == null) {
            fail("Nobody is waiting.");
            return;
        }
        notice = "Served #" + served.getEntryNo() + " - "
                + served.getBooking().getMember().getName()
                + " (booking " + served.getBooking().getConfirmationNo() + "). "
                + control.size() + " still waiting.";
    }

    private void addRequest() {
        Booking[] candidates = control.getQueueableBookings();
        if (candidates.length == 0) {
            fail("No confirmed booking is waiting for a room.");
            return;
        }

        System.out.println("\nConfirmed bookings not yet queued:");
        for (int i = 0; i < candidates.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, candidates[i]);
        }
        int pick = InputHelper.readInt("\nSelect booking (0 to go back) > ");
        if (pick == 0) {
            return;
        }
        if (pick < 0 || pick > candidates.length) {
            fail("Please enter a number between 0 and " + candidates.length + ".");
            return;
        }

        SpecialCategory[] bands = SpecialCategory.values();
        System.out.println("\nSpecial category:");
        for (int i = 0; i < bands.length; i++) {
            System.out.printf("[%d] %s%n", i + 1,
                    bands[i] == SpecialCategory.NONE ? "None" : bands[i].toString());
        }
        int band = InputHelper.readInt("\nSelect category (0 to go back) > ");
        if (band == 0) {
            return;
        }
        if (band < 0 || band > bands.length) {
            fail("Please enter a number between 0 and " + bands.length + ".");
            return;
        }

        Allocation added = control.enqueue(candidates[pick - 1], bands[band - 1]);
        if (added == null) {
            fail("That booking could not join the queue.");
            return;
        }
        notice = "Queued as #" + added.getEntryNo() + " at minute " + added.getArrivalMinute() + ".";
    }

    private void cancelRequest() {
        String confirmationNo = InputHelper.readLine("\nConfirmation no. to cancel (0 to go back) > ");
        if (confirmationNo.equals("0")) {
            return;
        }
        if (confirmationNo.isEmpty()) {
            fail("A confirmation number is required.");
            return;
        }

        Allocation entry = control.findByConfirmationNo(confirmationNo);
        if (entry == null) {
            fail("No queued request carries that confirmation number.");
            return;
        }
        if (!control.cancel(entry)) {
            fail("Could not remove that request.");
            return;
        }
        notice = "Removed #" + entry.getEntryNo() + " from the queue.";
    }

    /** Breaks off the prompt line first, so the message never lands beside it. */
    private void fail(String message) {
        System.out.println();
        OutputHelper.printErr(message);
        InputHelper.waitForEnter();
    }
}
