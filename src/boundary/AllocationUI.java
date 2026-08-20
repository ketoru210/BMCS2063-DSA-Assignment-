package boundary;

import control.AllocationControl;
import control.AllocationControl.Field;
import control.AllocationControl.Forecast;
import control.AllocationControl.Operator;
import control.AllocationControl.SearchKey;
import entity.Allocation;
import entity.Booking;
import entity.LoyaltyTier;
import entity.Room;
import entity.RoomType;
import entity.SpecialCategory;
import java.time.format.DateTimeFormatter;
import utility.InputHelper;
import utility.MenuItem;
import utility.OutputHelper;
import utility.TableRenderer;
import utility.TableRenderer.Align;
import utility.TreeRenderer;

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
 * @author Lam Yong Zhe
 */
public class AllocationUI {

    private static final String TITLE = "VIP & Loyalty Tier-Priority Room Allocation";
    private static final int MIN_CELL = 8;
    private static final String DEAD_SCORE = "N/A";
    private static final DateTimeFormatter STAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

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
        CANCEL("Cancel a Request"),
        REPRIORITISE("Reprioritise Changed Tiers"),
        SEARCH("Search the Queue"),
        FILTER("Filter the Queue"),
        STARVATION("Report: Overtake Risk"),
        FORECAST("Report: Fulfilment Forecast");

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
        // entering the screen always starts on a clean one
        boolean clearFirst = true;

        for (;;) {
            MenuOption selected = render(clearFirst);

            // the four queue actions are meant to be compared against the draw
            // above them, so their result stays on screen and the next draw
            // scrolls under it; everything else has filled the screen with its
            // own output and is wiped
            clearFirst = selected != MenuOption.FAST_FORWARD
                    && selected != MenuOption.SERVE
                    && selected != MenuOption.ADD
                    && selected != MenuOption.CANCEL
                    && selected != MenuOption.REPRIORITISE;

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
                case REPRIORITISE:
                    reprioritiseQueue();
                    break;
                case SEARCH:
                    searchQueue();
                    break;
                case FILTER:
                    filterQueue();
                    break;
                case STARVATION:
                    starvationReport();
                    break;
                case FORECAST:
                    forecastReport();
                    break;
            }
        }
    }

    // --- screen ---

    /** Redraws the queue with the menu underneath, and returns a valid choice. */
    private MenuOption render(boolean clearFirst) {
        MenuOption[] options = MenuOption.values();
        String[] labels = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labels[i] = options[i].label();
        }

        for (;;) {
            if (clearFirst) {
                OutputHelper.clearScreen();
            } else {
                System.out.println("\n\n");
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

        OutputHelper.printBlue("now = " + control.getClockMinute() + " min (simulated)"
                + "    rooms ready: " + control.getReadyRoomCount());
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
     * Draws the queue as the binary tree the heap actually is. Level order is
     * already the array order, so no traversal and no node objects are needed.
     * The root is highlighted because it is always served next.
     */
    private void printTree(Allocation[] levelOrder, Allocation[] serveOrder) {
        String[] slots = new String[levelOrder.length];
        for (int i = 0; i < levelOrder.length; i++) {
            slots[i] = labelOf(levelOrder[i], serveOrder);
        }

        // the heap knows its own depth; the drawing does not recompute it
        String[] rows = TreeRenderer.renderComplete(slots, control.getQueueHeight(), MIN_CELL);
        for (int i = 0; i < rows.length; i++) {
            if (i == 0) {
                OutputHelper.printOK(rows[i]);
            } else {
                System.out.println(rows[i]);
            }
        }

        System.out.println();
        OutputHelper.printBlue("  #n = entry no. (permanent)    (n) = serve order");
        OutputHelper.printBlue("  a score of " + DEAD_SCORE + " means a special category, which outranks every score");
        OutputHelper.printOK("  next serve: #" + serveOrder[0].getEntryNo());
    }

    private void printTable(Allocation[] serveOrder) {
        String[] headers = {"Serve", "Entry", "Conf no.", "Name", "Tier", "Special", "Waited",
                "Score at start", "Score now"};
        Align[] aligns = {Align.RIGHT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT,
                Align.RIGHT, Align.RIGHT, Align.RIGHT};

        String[][] cells = new String[serveOrder.length][];
        for (int i = 0; i < serveOrder.length; i++) {
            Allocation entry = serveOrder[i];
            boolean scored = entry.getCategory() == SpecialCategory.NONE;

            cells[i] = new String[]{
                String.valueOf(i + 1),
                "#" + entry.getEntryNo(),
                entry.getBooking().getConfirmationNo(),
                entry.getBooking().getMember().getName(),
                String.valueOf(entry.getTier()),
                String.valueOf(entry.getCategory()),
                (control.getClockMinute() - entry.getArrivalMinute()) + " min",
                scored ? String.valueOf(entry.getInvariantPriority()) : DEAD_SCORE,
                scored ? String.valueOf(control.livePriority(entry)) : DEAD_SCORE
            };
        }

        String[] rows = TableRenderer.render(headers, cells, aligns);
        OutputHelper.printBlue(rows[0]);
        for (int i = 1; i < rows.length; i++) {
            // the first entry is the root of the tree above — same entry, same color
            if (i == 1) {
                OutputHelper.printOK(rows[i]);
            } else {
                System.out.println(rows[i]);
            }
        }
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
        for (;;) {
            int minutes = InputHelper.readInt("\nFast-forward by how many minutes (0 = back) > ");
            if (minutes == 0) {
                return;
            }
            if (minutes < 0) {
                OutputHelper.printErr("Minutes must be positive.");
                continue;
            }
            control.fastForward(minutes);
            notice = "Clock is now at " + control.getClockMinute() + " min.";
            OutputHelper.printOK(notice);
        }
    }

    private void serveNext() {
        Allocation served = control.serveNext();
        String swept = sweptNote(control.getLastDiscarded());

        if (served == null) {
            // whatever was dropped, the head that is left still says why we stopped
            Allocation waiting = control.peekNext();
            fail((waiting == null
                    ? "Nobody is waiting."
                    : "No ready room fits #" + waiting.getEntryNo() + "'s "
                            + waiting.getBooking().getRoomType()
                            + " booking - a guest is never downgraded.") + swept);
            return;
        }

        Booking booking = served.getBooking();
        Room room = booking.getRoom();
        String upgrade = room.getRoomType() == booking.getRoomType()
                ? ""
                : " (upgraded from " + booking.getRoomType() + ")";

        notice = "Served #" + served.getEntryNo() + " - " + booking.getMember().getName()
                + " -> room " + room.getRoomNo() + ", " + room.getRoomType() + upgrade
                + ". " + control.size() + " still waiting." + swept;
    }

    private String sweptNote(int discarded) {
        if (discarded == 0) {
            return "";
        }
        return " Dropped " + discarded + (discarded == 1 ? " entry" : " entries")
                + " whose booking had already moved on.";
    }

    /**
     * Queues one booking after another until the desk backs out. The candidate
     * list is rebuilt every round, so a booking just queued drops off it.
     */
    private void addRequest() {
        for (;;) {
            Booking[] candidates = control.getQueueableBookings();
            if (candidates.length == 0) {
                fail("No confirmed booking is waiting for a room.");
                return;
            }

            String[][] cells = new String[candidates.length][];
            for (int i = 0; i < candidates.length; i++) {
                Booking booking = candidates[i];
                cells[i] = new String[]{
                    String.valueOf(i + 1),
                    booking.getConfirmationNo(),
                    booking.getMember().getName(),
                    String.valueOf(booking.getRoomType()),
                    booking.getCheckIn().format(STAY_DATE)
                            + " - " + booking.getCheckOut().format(STAY_DATE),
                    String.valueOf(booking.getNights()),
                    booking.getStatus()
                };
            }
            int pick = askFromTable("Confirmed bookings not yet queued:",
                    new String[]{"No.", "Conf No.", "Customer", "Room Type", "Stay", "Nights", "Status"},
                    cells,
                    new Align[]{Align.RIGHT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT,
                        Align.RIGHT, Align.LEFT});
            if (pick < 0) {
                return;
            }

            SpecialCategory[] bands = SpecialCategory.values();
            String[] bandLabels = new String[bands.length];
            for (int i = 0; i < bands.length; i++) {
                bandLabels[i] = bands[i] == SpecialCategory.NONE ? "None" : bands[i].toString();
            }
            int band = askFrom("Special category:", bandLabels);
            if (band < 0) {
                // backing out of the category only undoes the booking just picked
                continue;
            }

            Allocation added = control.enqueue(candidates[pick], bands[band]);
            if (added == null) {
                OutputHelper.printErr("That booking could not join the queue.");
                continue;
            }
            notice = "Queued as #" + added.getEntryNo()
                    + " at minute " + added.getArrivalMinute() + ".";
            System.out.println();
            OutputHelper.printOK(notice);
        }
    }

    /**
     * A wrong confirmation number asks again rather than dropping back to the menu:
     * the desk is reading it off a guest, and a typo should not cost the screen.
     */
    private void cancelRequest() {
        for (;;) {
            String confirmationNo = InputHelper.readLine("\nConfirmation no. to cancel (0 = back) > ");
            if (confirmationNo.equals("0")) {
                return;
            }
            if (confirmationNo.isEmpty()) {
                OutputHelper.printErr("A confirmation number is required.");
                continue;
            }

            Allocation entry = control.findByConfirmationNo(confirmationNo);
            if (entry == null) {
                OutputHelper.printErr("No queued request carries that confirmation number.");
                continue;
            }
            if (!control.cancel(entry)) {
                OutputHelper.printErr("Could not remove that request.");
                continue;
            }
            notice = "Removed #" + entry.getEntryNo() + " from the queue.";
            return;
        }
    }

    /**
     * Re-keys the entries whose member changed tier after they joined. Kept on
     * screen rather than clearing, so the redraw underneath shows them move.
     */
    private void reprioritiseQueue() {
        int outdated = control.countOutdatedKeys();
        if (outdated == 0) {
            notice = "Every entry is keyed on its member's current tier.";
            return;
        }
        int moved = control.reprioritise();
        notice = "Re-keyed " + moved + (moved == 1 ? " entry" : " entries")
                + " whose tier changed since admission.";
    }

    // --- reports ---

    /**
     * Asks what to measure and how to arrange it, then hands both answers to the
     * control. Nothing about the question is fixed here: the field, the direction
     * and the comparison all arrive as values.
     */
    /**
     * Finds a waiting request the way the desk would be asked for one - by whom
     * it is for, not by its score - and answers the question that actually
     * follows: how far down the queue it is.
     */
    private void searchQueue() {
        for (;;) {
            OutputHelper.printTitle("Search the Queue");
            if (control.isEmpty()) {
                fail("Nobody is waiting.");
                return;
            }

            SearchKey key = askSearchKey();
            if (key == null) {
                return;
            }

            // only a name is worth matching on a fragment; the two numbers are exact
            String prompt = key + (key == SearchKey.CUSTOMER_NAME ? " (part of it is enough)" : "");
            String term = InputHelper.readLine("\n" + prompt + " > ");

            Allocation[] hits = control.search(key, term);
            if (hits.length == 0) {
                System.out.println();
                OutputHelper.printErr("No request in the queue has that " + key + ".");
                continue;
            }

            System.out.println();
            OutputHelper.printBlue("matched " + hits.length + " of " + control.size() + " waiting");
            System.out.println();
            printHits(hits);

            if (hits.length == 1) {
                int position = control.getServePosition(hits[0]);
                System.out.println();
                OutputHelper.printOK("  served " + position + " of " + control.size()
                        + "    " + (position - 1) + " ahead, "
                        + (control.size() - position) + " behind");
            }
            InputHelper.waitForEnter();
        }
    }

    private void printHits(Allocation[] hits) {
        String[] headers = {"Serve", "Entry", "Name", "Tier", "Special", "Waited", "Score now"};
        Align[] aligns = {Align.RIGHT, Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT,
                Align.RIGHT, Align.RIGHT};

        String[][] cells = new String[hits.length][];
        for (int i = 0; i < hits.length; i++) {
            Allocation entry = hits[i];
            boolean scored = entry.getCategory() == SpecialCategory.NONE;
            cells[i] = new String[]{
                String.valueOf(control.getServePosition(entry)),
                "#" + entry.getEntryNo(),
                entry.getBooking().getMember().getName(),
                String.valueOf(entry.getTier()),
                String.valueOf(entry.getCategory()),
                control.getWaited(entry) + " min",
                scored ? String.valueOf(control.livePriority(entry)) : DEAD_SCORE
            };
        }

        String[] table = TableRenderer.render(headers, cells, aligns);
        OutputHelper.printBlue(table[0]);
        for (int i = 1; i < table.length; i++) {
            System.out.println(table[i]);
        }
    }

    /**
     * Narrows the live queue on one measure and reports what the survivors add up
     * to, so the answer is a shape of the queue rather than a listing of it.
     */
    private void filterQueue() {
        for (;;) {
            OutputHelper.printTitle("Filter the Queue");
            if (control.isEmpty()) {
                fail("Nobody is waiting.");
                return;
            }

            Field field = askField();
            if (field == null) {
                return;
            }

            // only the exposure figure depends on who might turn up next
            LoyaltyTier challenger = LoyaltyTier.PLATINUM;
            if (field == Field.EXPOSURE) {
                challenger = askChallenger();
                if (challenger == null) {
                    // a sub-step back only restarts the question, it does not leave
                    continue;
                }
            }

            Operator[] ops = Operator.values();
            int comparison = askComparison(field, ops, "Back");
            if (comparison == 0) {
                continue;
            }
            Long chosen = askThreshold(field, challenger);
            if (chosen == null) {
                continue;
            }
            long threshold = chosen;

            Allocation[] rows = control.filterBy(control.getServeOrder(), field,
                    ops[comparison - 1], threshold, challenger);
            printFilter(rows, field, ops[comparison - 1], threshold);
            InputHelper.waitForEnter();
        }
    }

    private void printFilter(Allocation[] rows, Field field, Operator op, long threshold) {
        System.out.println();
        OutputHelper.printBlue("now = " + control.getClockMinute() + " min (simulated)"
                + "    filter: " + field + " " + op + " " + describe(field, threshold));
        System.out.println();

        if (rows.length == 0) {
            OutputHelper.printBlue("  No entry in the queue has " + field + " " + op + " " + describe(field, threshold) + ".");
            return;
        }

        printHits(rows);

        long waited = 0;
        for (int i = 0; i < rows.length; i++) {
            waited += control.getWaited(rows[i]);
        }
        double share = rows.length * 100.0 / control.size();

        System.out.println();
        OutputHelper.printBlue(String.format("  %d of %d waiting (%.1f%%)    mean wait %.1f min",
                rows.length, control.size(), share, waited / (double) rows.length));
        OutputHelper.printOK("  first of them to be served: #" + rows[0].getEntryNo()
                + " at position " + control.getServePosition(rows[0]));
    }

    /** Returns the 1-based operator choice, 0 for whatever the caller offers as 0, -1 if out of range. */
    private int askComparison(Field field, Operator[] ops, String zeroLabel) {
        System.out.println("\nKeep only the entries whose " + field + " is:");
        System.out.println("[0] " + zeroLabel);
        for (int i = 0; i < ops.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, ops[i]);
        }
        for (;;) {
            int comparison = InputHelper.readInt("\nPlease Select > ");
            if (comparison >= 0 && comparison <= ops.length) {
                return comparison;
            }
            OutputHelper.printErr("Please enter a number between 0 and " + ops.length + ".");
        }
    }

    /**
     * Answers one question - who a fresh arrival would get in front of - and says
     * so before the table, because the table is the evidence and not the answer.
     * General filtering lives on the Filter screen, so nothing is asked here that
     * the report does not need.
     */
    private void starvationReport() {
        OutputHelper.printTitle("Overtake Risk");
        if (control.isEmpty()) {
            fail("Nobody is waiting.");
            return;
        }

        LoyaltyTier challenger = askTier("If somebody walked in right now, what tier would they be?");
        if (challenger == null) {
            return;
        }

        // most at risk first: that ordering is the report's whole point
        Allocation[] rows = control.sortBy(control.getServeOrder(), Field.EXPOSURE, true, challenger);
        printStarvation(rows, challenger);
        InputHelper.waitForEnter();
    }

    private void printStarvation(Allocation[] rows, LoyaltyTier challenger) {
        System.out.println();
        OutputHelper.printReportHeader("Overtake Risk Report",
                "challenger tier = " + challenger + ", clock at " + control.getClockMinute() + " min");
        OutputHelper.printBlue("A " + challenger + " customer walks in at minute " + control.getClockMinute()
                + " - who already waiting would they get in front of?");
        OutputHelper.printBlue("Waiting earns priority as it goes, so every request eventually becomes"
                + " impossible to overtake. That is the anti-starvation guarantee.");
        System.out.println();

        int safe = control.countProtected(challenger);
        int exposed = control.size() - safe;
        Allocation worst = control.getMostExposed(challenger);

        if (exposed == 0) {
            OutputHelper.printOK("  Nobody would be overtaken. Every request waiting has already"
                    + " out-waited a fresh " + challenger + ".");
        } else {
            OutputHelper.printErr("  " + exposed + " of " + control.size()
                    + " would be overtaken by that arrival.");
            OutputHelper.printErr("  Worst placed is #" + worst.getEntryNo() + " "
                    + worst.getBooking().getMember().getName() + ", overtakeable for the next "
                    + control.getExposure(worst, challenger) + " min - that is, until the clock reads "
                    + control.getSealedAtMinute(challenger) + ".");
            OutputHelper.printBlue("  Nobody in the queue as it stands can be overtaken after that.");
        }
        OutputHelper.printOK("  " + safe + " of " + control.size() + " are already safe.");

        System.out.println();
        String[] headers = {"Entry", "Name", "Tier", "Special", "Waited", "Safe from minute", "Exposure left"};
        Align[] aligns = {Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT,
                Align.RIGHT, Align.RIGHT, Align.RIGHT};

        String[][] cells = new String[rows.length][];
        boolean[] settled = new boolean[rows.length];
        for (int i = 0; i < rows.length; i++) {
            Allocation entry = rows[i];
            boolean protectedByCategory = control.isCategoryProtected(entry);
            int exposure = control.getExposure(entry, challenger);
            settled[i] = exposure == 0;

            cells[i] = new String[]{
                "#" + entry.getEntryNo(),
                entry.getBooking().getMember().getName(),
                String.valueOf(entry.getTier()),
                String.valueOf(entry.getCategory()),
                control.getWaited(entry) + " min",
                protectedByCategory ? DEAD_SCORE
                        : String.valueOf(control.getImmuneAt(entry, challenger)),
                protectedByCategory ? "never at risk"
                        : (exposure == 0 ? "safe" : exposure + " min")
            };
        }

        String[] table = TableRenderer.render(headers, cells, aligns);
        OutputHelper.printBlue(table[0]);
        for (int i = 1; i < table.length; i++) {
            if (settled[i - 1]) {
                OutputHelper.printOK(table[i]);
            } else {
                System.out.println(table[i]);
            }
        }

        System.out.println();
        OutputHelper.printBlue("  Safe from minute = the clock reading after which this request"
                + " can no longer be overtaken");
        OutputHelper.printBlue("  Exposure left    = how much longer it stays overtakeable, counted from now");
        OutputHelper.printBlue("  " + DEAD_SCORE + " = ranked on special category, which no ordinary arrival reaches");

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Records : %d waiting    Exposed : %d    Safe : %d", rows.length, exposed, safe));
    }

    private void forecastReport() {
        if (control.isEmpty()) {
            fail("Nobody is waiting.");
            return;
        }

        int lookAhead = InputHelper.readInt(
                "\nLook ahead how many serves (0 = until the queue stalls) > ");
        if (lookAhead < 0) {
            fail("Look-ahead cannot be negative.");
            return;
        }

        printForecast(control.forecast(lookAhead), lookAhead);
        InputHelper.waitForEnter();
    }

    private void printForecast(Forecast forecast, int lookAhead) {
        System.out.println();
        OutputHelper.printReportHeader("Fulfilment Forecast Report",
                "look-ahead = " + (lookAhead == 0 ? "whole queue (" + forecast.getQueued() + " waiting)"
                        : lookAhead + " serves")
                        + ", rooms ready = " + control.getReadyRoomCount());
        System.out.println();

        Allocation blocker = forecast.getBlocker();
        if (blocker == null) {
            OutputHelper.printOK(String.format("  Served    : %d of %d - the look-ahead clears with no stall",
                    forecast.getServed(), forecast.getQueued()));
        } else {
            OutputHelper.printOK(String.format("  Served    : %d of %d before the queue stalls",
                    forecast.getServed(), forecast.getQueued()));
            OutputHelper.printErr(String.format("  Stalls at : #%d %s - wants %s, and no upgrade is free either",
                    blocker.getEntryNo(),
                    blocker.getBooking().getMember().getName(),
                    blocker.getBooking().getRoomType()));
            System.out.printf("  Blocked   : %d %s behind it%n",
                    forecast.getBlocked(),
                    forecast.getBlocked() == 1 ? "entry sits" : "entries sit");
        }

        System.out.println();
        RoomType[] types = RoomType.values();
        int[] waiting = forecast.getWaitingByType();
        int[] ready = forecast.getReadyByType();

        String[] headers = {"Type", "Waiting", "Ready", "Gap"};
        Align[] aligns = {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT};
        String[][] cells = new String[types.length][];
        boolean[] short_ = new boolean[types.length];
        for (int i = 0; i < types.length; i++) {
            int gap = ready[i] - waiting[i];
            short_[i] = gap < 0;
            cells[i] = new String[]{
                String.valueOf(types[i]),
                String.valueOf(waiting[i]),
                String.valueOf(ready[i]),
                String.valueOf(gap)
            };
        }

        String[] table = TableRenderer.render(headers, cells, aligns);
        System.out.println("  " + table[0]);
        for (int i = 1; i < table.length; i++) {
            if (short_[i - 1]) {
                OutputHelper.printErr("  " + table[i]);
            } else {
                System.out.println("  " + table[i]);
            }
        }
        OutputHelper.printBlue("  Gap = ready rooms minus requests waiting for them; a negative gap"
                + " is a shortage, and is what stalls the queue.");

        System.out.println();
        System.out.printf("  Upgrades  : %d, giving away RM %,.2f%n",
                forecast.getUpgrades(), forecast.getUpgradeCost());
        OutputHelper.printBlue("  An upgrade is a request served in a dearer room because its own type"
                + " had run out; the money is the rate difference the resort absorbs.");

        if (blocker != null) {
            Room releasable = forecast.getReleasable();
            OutputHelper.printBlue(String.format("  Unblock   : one more ready %s serves %d more; %s",
                    blocker.getBooking().getRoomType(),
                    forecast.getUnblockGain(),
                    releasable == null
                            ? "none is vacant, so this waits on a checkout"
                            : "room " + releasable.getRoomNo() + " is vacant and only needs cleaning ("
                                    + releasable.getHousekeepingStatus() + ")"));
        }

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Records : %d waiting    Served : %d    Blocked : %d    Upgrades : %d (RM %,.2f)",
                forecast.getQueued(), forecast.getServed(), forecast.getBlocked(),
                forecast.getUpgrades(), forecast.getUpgradeCost()));
    }

    /**
     * Numbered picker with the same shape as the menus: {@code [0]} backs out and
     * returns -1, anything out of range is asked again rather than thrown away.
     */
    private int askFrom(String title, String[] labels) {
        System.out.println("\n" + title);
        System.out.println("[0] Back");
        for (int i = 0; i < labels.length; i++) {
            System.out.println("[" + (i + 1) + "] " + labels[i]);
        }
        for (;;) {
            int pick = InputHelper.readInt("\nPlease Select > ");
            if (pick == 0) {
                return -1;
            }
            if (pick >= 1 && pick <= labels.length) {
                return pick - 1;
            }
            OutputHelper.printErr("Please enter a number between 0 and " + labels.length + ".");
        }
    }

    /**
     * As above, but the choices are rows of a bordered table rather than single
     * labels, so a multi-column list keeps its headings instead of running
     * together. The first column is expected to be the number the caller types.
     * Returns -1 when the user backs out.
     */
    private int askFromTable(String title, String[] headers, String[][] cells, Align[] aligns) {
        System.out.println("\n" + title);
        String[] table = TableRenderer.renderBordered(headers, cells, aligns);
        for (int i = 0; i < table.length; i++) {
            System.out.println(table[i]);
        }
        System.out.println("[0] Back");
        for (;;) {
            int pick = InputHelper.readInt("\nPlease Select > ");
            if (pick == 0) {
                return -1;
            }
            if (pick >= 1 && pick <= cells.length) {
                return pick - 1;
            }
            OutputHelper.printErr("Please enter a number between 0 and " + cells.length + ".");
        }
    }

    /** Null means the user backed out. */
    private SearchKey askSearchKey() {
        SearchKey[] keys = SearchKey.values();
        String[] labels = new String[keys.length];
        for (int i = 0; i < keys.length; i++) {
            labels[i] = String.valueOf(keys[i]);
        }
        int pick = askFrom("Search on which of them?", labels);
        return pick < 0 ? null : keys[pick];
    }

    /** Null means the user backed out. */
    private LoyaltyTier askTier(String question) {
        LoyaltyTier[] tiers = LoyaltyTier.values();
        String[] labels = new String[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            labels[i] = tiers[i] + " (worth " + tiers[i].getWeight() + " min of waiting)";
        }
        int pick = askFrom(question, labels);
        return pick < 0 ? null : tiers[pick];
    }

    private LoyaltyTier askChallenger() {
        return askTier("Test the queue against the arrival of which tier?");
    }

    /**
     * Tier weight is a band, not a dial - four values and nothing in between - so
     * that one measure is answered by naming a tier rather than by typing the
     * number the tier happens to carry. Null means the user backed out.
     */
    private Long askThreshold(Field field, LoyaltyTier challenger) {
        if (field == Field.TIER_WEIGHT) {
            LoyaltyTier tier = askTier("At which tier?");
            return tier == null ? null : (long) tier.getWeight();
        }
        return (long) InputHelper.readInt(scaleOf(field, challenger) + " > ");
    }

    /** Shows a threshold the way it was chosen, not the way the field stores it. */
    private String describe(Field field, long threshold) {
        if (field == Field.TIER_WEIGHT) {
            LoyaltyTier[] tiers = LoyaltyTier.values();
            for (int i = 0; i < tiers.length; i++) {
                if (tiers[i].getWeight() == threshold) {
                    return tiers[i] + " (" + threshold + " min)";
                }
            }
        }
        return String.valueOf(threshold);
    }

    /** Null means the user backed out; a number out of range is simply asked again. */
    private Field askField() {
        Field[] fields = Field.values();
        System.out.println("\nMeasure which value?");
        System.out.println("[0] Back");
        for (int i = 0; i < fields.length; i++) {
            System.out.printf("[%d] %s%n", i + 1, fields[i]);
        }
        for (;;) {
            int pick = InputHelper.readInt("\nPlease Select > ");
            if (pick == 0) {
                return null;
            }
            if (pick >= 1 && pick <= fields.length) {
                return fields[pick - 1];
            }
            OutputHelper.printErr("Please enter a number between 0 and " + fields.length + ".");
        }
    }

    /**
     * Names the measure and the span it actually covers in the queue right now, so
     * the number being asked for arrives with a scale attached rather than as a
     * bare "Value".
     */
    private String scaleOf(Field field, LoyaltyTier challenger) {
        Allocation[] entries = control.getServeOrder();
        if (entries.length == 0) {
            return String.valueOf(field);
        }
        long low = control.fieldValue(entries[0], field, challenger);
        long high = low;
        for (int i = 1; i < entries.length; i++) {
            long value = control.fieldValue(entries[i], field, challenger);
            low = Math.min(low, value);
            high = Math.max(high, value);
        }
        String unit = (field == Field.ENTRY_NO || field == Field.LIVE_PRIORITY) ? "" : " min";
        return field + unit + " (queue now runs " + low + " to " + high + ")";
    }

    /** Breaks off the prompt line first, so the message never lands beside it. */
    private void fail(String message) {
        System.out.println();
        OutputHelper.printErr(message);
        InputHelper.waitForEnter();
    }
}
