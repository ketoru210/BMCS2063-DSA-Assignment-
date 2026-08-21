package boundary;

import control.BookingControl;
import control.BookingControl.SortKey;
import entity.Booking;
import entity.LoyaltyTier;
import entity.Member;
import entity.RoomType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import utility.InputHelper;
import utility.MenuItem;
import utility.OutputHelper;
import utility.ChartRenderer;
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
    // a party larger than this is a group booking, which the desk handles off-system
    private static final int MAX_ROOMS_PER_REQUEST = 10;
    private static final int MAX_LOOKAHEAD_DAYS = 30;
    private static final DateTimeFormatter STAY_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final BookingControl control;

    private String notice;

    public FrontDeskUI(BookingControl control) {
        this.control = control;
    }

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        NEW_BOOKING("New Booking"),
        LOOK_UP("Look Up Booking"),
        LIST("View All Bookings"),
        CHECK_IN("Check-in Guest"),
        CHECK_OUT("Check-out Guest"),
        CANCEL("Cancel Booking"),
        PURGE("Purge Cancelled Bookings"),
        HEALTH("Inspect Tree Balance"),
        ARRIVALS("Report: Guests Arriving vs Rooms Ready"),
        RANGE("Report: Bookings and Money by Number Range");

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
                    newBooking();
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
                    inspectTree();
                    break;
                case ARRIVALS:
                    arrivalReadinessReport();
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

    /**
     * One door for both kinds of booking. They differ in exactly one place - a
     * member may name a future date, a walk-in is at the counter and so starts
     * today - and that is not enough to justify two screens the desk has to choose
     * between before knowing which one the caller is.
     * <p>
     * Whoever is booking is asked for once and then held, because a caller books
     * for a party rather than for one bed: each round through the loop adds
     * another request, and the batch is receipted together at the end.
     */
    private void newBooking() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("New Booking");

        step(1, "Who is booking?");
        Member member = pickGuest();
        if (member == null) {
            return;
        }
        printMemberCard(member);

        Booking[] batch = new Booking[0];
        // lines, not rooms: one line can be three rooms of the same type
        int requests = 0;

        for (;;) {
            System.out.println();
            OutputHelper.printBlue("== Line " + (requests + 1)
                    + " for " + member.getName() + " ==");
            if (requests == 0) {
                System.out.println("A line is one room type over one set of dates."
                        + " Add as many lines as the party needs.");
            }

            step(2, "Choose a room type");
            RoomType roomType = readRoomType();
            if (roomType == null) {
                // backing out of the type is how the desk says the party is complete
                break;
            }
            int rooms = readRooms(roomType);
            if (rooms == 0) {
                continue;
            }

            step(3, "Set the stay dates");
            LocalDate checkIn;
            if (control.isWalkIn(member)) {
                checkIn = LocalDate.now();
                System.out.println("Check-in date : " + checkIn.format(STAY_DATE)
                        + "  (a walk-in is at the counter, so the stay starts today)");
            } else {
                checkIn = readDate("Check-in date (dd-MM-yyyy, blank = today, 0 = back) > ");
                if (checkIn == null) {
                    continue;
                }
            }
            int nights = readNights();
            if (nights == 0) {
                continue;
            }
            LocalDate checkOut = checkIn.plusDays(nights);

            // a walk-in record is minutes old, so any overlap it has is this very
            // party being built up room by room - there is nothing to warn about
            if (!control.isWalkIn(member) && !overlapAccepted(member, checkIn, checkOut)) {
                continue;
            }

            step(4, "Confirm");
            if (!confirmLine(member, roomType, checkIn, checkOut, nights, rooms)) {
                continue;
            }

            Booking[] made = control.createBookings(member, roomType, checkIn, checkOut, rooms);
            if (made.length == 0) {
                OutputHelper.printErr("Could not create the booking with those details.");
                InputHelper.waitForEnter();
                continue;
            }
            batch = append(batch, made);
            requests++;

            OutputHelper.printOK("Added " + made.length + " booking"
                    + (made.length == 1 ? "" : "s") + ". " + batch.length + " so far.");
            String more = InputHelper.readLine("Add another line - a different room type"
                    + " or different dates - for " + member.getName() + "? [y/N] > ");
            if (!more.equalsIgnoreCase("y")) {
                break;
            }
        }

        if (batch.length == 0) {
            notice = "No booking was made for " + member.getName() + ".";
            return;
        }
        printBatchReceipt(member, batch);
        notice = "Booked " + batch.length + " room" + (batch.length == 1 ? "" : "s") + " for "
                + member.getName() + " (" + money(control.totalRevenueOf(batch))
                + "). They now wait for rooms in VIP & Loyalty Tier-Priority Room Allocation.";
    }

    /**
     * An overlapping stay is a warning and not a refusal: a member booking for a
     * party legitimately holds several rooms over the same nights, and only the
     * desk can tell that apart from the same booking being taken down twice.
     */
    private boolean overlapAccepted(Member member, LocalDate checkIn, LocalDate checkOut) {
        Booking clash = control.findClashingStay(member, checkIn, checkOut);
        if (clash == null) {
            return true;
        }
        OutputHelper.printBlue(member.getName() + " already holds "
                + clash.getConfirmationNo() + " over those nights ("
                + clash.getCheckIn().format(STAY_DATE) + " - "
                + clash.getCheckOut().format(STAY_DATE) + ", " + clash.getStatus()
                + "). Normal for a party, a mistake if the stay was taken down twice.");
        return InputHelper.readLine("Book it anyway? [y/N] > ").equalsIgnoreCase("y");
    }

    private boolean confirmLine(Member member, RoomType roomType, LocalDate checkIn,
            LocalDate checkOut, int nights, int rooms) {
        double perRoom = roomType.getRatePerNight() * nights;
        System.out.println("  Booked by : " + member.getName()
                + (control.isWalkIn(member)
                        ? "  (walk-in guest)"
                        : "  (" + member.getMemberID() + ", " + member.getCurrentTier() + ")"));
        System.out.println("  Room type : " + roomType + "  at "
                + money(roomType.getRatePerNight()) + " / night");
        System.out.println("  Rooms     : " + rooms);
        System.out.println("  Stay      : " + checkIn.format(STAY_DATE) + " to "
                + checkOut.format(STAY_DATE) + "  (" + nights + " night"
                + (nights == 1 ? "" : "s") + ")");
        System.out.println("  Charge    : " + money(perRoom * rooms)
                + (rooms == 1 ? "" : "   (" + money(perRoom) + " per room)"));
        System.out.println();

        if (InputHelper.readLine("Confirm this line? [y/N] > ").equalsIgnoreCase("y")) {
            return true;
        }
        OutputHelper.printErr("Line abandoned.");
        return false;
    }

    /**
     * What the whole visit produced, and what it did to the room pool: a party of
     * four suites against one ready suite is the desk's problem now, not a surprise
     * for whoever runs the allocation queue later.
     */
    private void printBatchReceipt(Member member, Booking[] batch) {
        OutputHelper.clearScreen();
        OutputHelper.printReportHeader("Booking Receipt",
                (control.isWalkIn(member) ? "walk-in " : member.getMemberID() + " ")
                        + member.getName() + ", " + batch.length
                        + " room" + (batch.length == 1 ? "" : "s"));
        System.out.println();
        printBookings(null, batch);

        RoomType[] types = RoomType.values();
        int[] booked = control.roomsByType(batch);
        int[] ready = control.readyByType();
        int[] waiting = control.waitingByType();

        System.out.println();
        System.out.println("  Pressure on the room pool");
        String[] headers = {"ROOM TYPE", "BOOKED", "READY NOW", "ALREADY QUEUED", "CHASING 1 ROOM"};
        Align[] aligns = {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT, Align.RIGHT};
        String[][] cells = new String[types.length][];
        for (int i = 0; i < types.length; i++) {
            // a new booking is only a candidate for M2's queue, not yet in it, so
            // the batch is pressure on top of whatever is already queued
            int chasing = booked[i] + waiting[i];
            cells[i] = new String[]{
                String.valueOf(types[i]),
                String.valueOf(booked[i]),
                String.valueOf(ready[i]),
                String.valueOf(waiting[i]),
                ready[i] == 0 ? (chasing == 0 ? "-" : "no room")
                        : String.format("%.1f", (double) chasing / ready[i])
            };
        }
        String[] table = TableRenderer.render(headers, cells, aligns);
        for (int i = 0; i < table.length; i++) {
            System.out.println("    " + table[i]);
        }

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Rooms : %d    Total charge : %s    Average : %s per room",
                batch.length, money(control.totalRevenueOf(batch)),
                money(control.totalRevenueOf(batch) / batch.length)));
        InputHelper.waitForEnter();
    }

    /** Grown by copying because the batch has no known size until the desk stops. */
    private Booking[] append(Booking[] batch, Booking[] more) {
        Booking[] grown = new Booking[batch.length + more.length];
        for (int i = 0; i < batch.length; i++) {
            grown[i] = batch[i];
        }
        for (int i = 0; i < more.length; i++) {
            grown[batch.length + i] = more[i];
        }
        return grown;
    }

    private void step(int number, String title) {
        System.out.println();
        OutputHelper.printBlue("-- Step " + number + " : " + title + " --");
    }

    /** Who the desk has landed on, and what the registry already holds for them. */
    private void printMemberCard(Member member) {
        System.out.println();
        OutputHelper.printOK("Booking for " + member.getName() + "  "
                + (control.isWalkIn(member)
                        ? "(walk-in guest, no membership)"
                        : "(" + member.getMemberID() + ", " + member.getCurrentTier()
                                + ", " + member.getCurrentPoints() + " pts)"));

        if (control.isWalkIn(member)) {
            // the record was made a second ago, so there is no history to show
            return;
        }
        Booking[] onFile = control.bookingsOf(member);
        if (onFile.length == 0) {
            System.out.println("No booking on file for this member yet.");
            return;
        }
        System.out.println(onFile.length + " booking" + (onFile.length == 1 ? "" : "s")
                + " already on file:");
        printBookings(null, onFile);
    }

    /**
     * Whoever the booking is for, member or not. Null means the user backed out.
     */
    private Member pickGuest() {
        System.out.println("[0] Back");
        System.out.println("[1] Existing member");
        System.out.println("[2] Walk-in guest");
        for (;;) {
            int pick = InputHelper.readInt("Please Select > ");
            if (pick == 0) {
                return null;
            }
            if (pick == 1) {
                return pickMember();
            }
            if (pick == 2) {
                return readGuest();
            }
            OutputHelper.printErr("Please enter 0, 1 or 2.");
        }
    }

    /** Null means the user backed out. */
    private Member readGuest() {
        for (;;) {
            String name = InputHelper.readLine("Guest name (0 = back) > ");
            if (name.equals("0")) {
                return null;
            }
            Member guest = control.createGuest(name);
            if (guest != null) {
                return guest;
            }
            OutputHelper.printErr("Guest name is required.");
        }
    }

    /** Null means the user backed out. */
    private Member pickMember() {
        for (;;) {
            String query = InputHelper.readLine(
                    "Member ID, username or name (e.g. 1, M00001, tanwm, Tan) (0 = back) > ");
            if (query.equals("0")) {
                return null;
            }
            if (query.isEmpty()) {
                OutputHelper.printErr("Enter something to search on.");
                continue;
            }

            Member[] matches = control.searchMembers(query);
            if (matches.length == 0) {
                OutputHelper.printErr("No member matches \"" + query
                        + "\". A guest who has not joined is a walk-in booking instead.");
                continue;
            }
            if (matches.length == 1) {
                return matches[0];
            }

            System.out.println();
            System.out.println(matches.length + " members match. Which one?");
            printMembers(matches);
            System.out.println("[0] Search again");
            for (;;) {
                int pick = InputHelper.readInt("Member > ");
                if (pick == 0) {
                    break;
                }
                if (pick >= 1 && pick <= matches.length) {
                    return matches[pick - 1];
                }
                OutputHelper.printErr("Please enter a number between 0 and " + matches.length + ".");
            }
        }
    }

    private void printMembers(Member[] members) {
        String[] headers = {"No.", "Member ID", "Name", "Tier", "Points"};
        Align[] aligns = {Align.LEFT, Align.LEFT, Align.LEFT, Align.LEFT, Align.RIGHT};
        String[][] cells = new String[members.length][];
        for (int i = 0; i < members.length; i++) {
            cells[i] = new String[]{
                String.valueOf(i + 1),
                members[i].getMemberID(),
                members[i].getName(),
                String.valueOf(members[i].getCurrentTier()),
                String.valueOf(members[i].getCurrentPoints())
            };
        }
        String[] table = TableRenderer.renderBordered(headers, cells, aligns);
        for (int i = 0; i < table.length; i++) {
            System.out.println(table[i]);
        }
    }

    /** 0 means the user backed out. */
    private int readRooms(RoomType roomType) {
        for (;;) {
            int rooms = InputHelper.readInt("How many " + roomType + " rooms? (0 = back) > ");
            if (rooms == 0) {
                return 0;
            }
            if (rooms >= 1 && rooms <= MAX_ROOMS_PER_REQUEST) {
                return rooms;
            }
            OutputHelper.printErr("Enter between 1 and " + MAX_ROOMS_PER_REQUEST
                    + " rooms, or 0 to go back.");
        }
    }

    /** 0 means the user backed out. */
    private int readNights() {
        for (;;) {
            int nights = InputHelper.readInt("Nights (0 = back) > ");
            if (nights == 0) {
                return 0;
            }
            if (nights >= 1) {
                return nights;
            }
            OutputHelper.printErr("Nights must be at least 1.");
        }
    }

    /** Null means the user backed out; a blank line means today. */
    private LocalDate readDate(String prompt) {
        for (;;) {
            String typed = InputHelper.readLine(prompt);
            if (typed.equals("0")) {
                return null;
            }
            if (typed.isEmpty()) {
                return LocalDate.now();
            }
            try {
                LocalDate date = LocalDate.parse(typed, STAY_DATE);
                if (date.isBefore(LocalDate.now())) {
                    OutputHelper.printErr("A stay cannot start in the past.");
                    continue;
                }
                return date;
            } catch (DateTimeParseException e) {
                OutputHelper.printErr("Please enter the date as dd-MM-yyyy.");
            }
        }
    }

    /**
     * The rate table with the room pool beside it, so the desk picks a type
     * knowing what is behind it instead of guessing and finding out at allocation.
     */
    private RoomType readRoomType() {
        RoomType[] types = RoomType.values();
        int[] ready = control.readyByType();
        int[] waiting = control.waitingByType();

        System.out.println();
        String[][] cells = new String[types.length][];
        for (int i = 0; i < types.length; i++) {
            cells[i] = new String[]{
                String.valueOf(i + 1),
                String.valueOf(types[i]),
                money(types[i].getRatePerNight()) + " / night",
                String.valueOf(ready[i]),
                String.valueOf(waiting[i])
            };
        }
        String[] menu = TableRenderer.renderBordered(
                new String[]{"No.", "Room Type", "Rate", "Ready", "Waiting"}, cells,
                new Align[]{Align.LEFT, Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT});
        for (int i = 0; i < menu.length; i++) {
            System.out.println(menu[i]);
        }
        OutputHelper.printBlue(
                "  Ready = free and cleaned as of now; Waiting = bookings already queued for it.");
        OutputHelper.printBlue(
                "  A stay booked for a later date is not held against these counts.");
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

    /**
     * A look at the shape of the registry rather than at its contents - how far
     * the tree has drifted from balanced, and what that costs a lookup.
     */
    private void inspectTree() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("Tree Balance");
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
        OutputHelper.printBlue(String.format(
                "  %d records    height %d of %d optimal    degeneracy %.1f%%",
                size, height, optimal, degeneracy));
        InputHelper.waitForEnter();
    }

    /**
     * Whether the guests due in can actually be housed. The arrivals come off the
     * registry, the tier off each booking's member, and the supply off the room
     * pool - three places, one question, which is what makes it a report rather
     * than another listing of bookings.
     */
    private void arrivalReadinessReport() {
        OutputHelper.printTitle("Guests Arriving vs Rooms Ready");
        int days = InputHelper.readInt("\nLook ahead how many days (0 = back) > ");
        if (days == 0) {
            return;
        }
        if (days < 0 || days > MAX_LOOKAHEAD_DAYS) {
            OutputHelper.printErr("Enter between 1 and " + MAX_LOOKAHEAD_DAYS + " days.");
            InputHelper.waitForEnter();
            return;
        }

        Booking[] arrivals = control.arrivalsWithin(days);
        if (arrivals.length == 0) {
            System.out.println();
            OutputHelper.printErr("No confirmed arrival is due in the next " + days
                    + (days == 1 ? " day." : " days."));
            InputHelper.waitForEnter();
            return;
        }

        RoomType[] types = RoomType.values();
        int[] wanted = control.roomsByType(arrivals);
        int[] ready = control.readyByType();
        Booking[] stranded = control.unhoused(arrivals);

        OutputHelper.clearScreen();
        OutputHelper.printReportHeader("Guests Arriving vs Rooms Ready",
                "check-in within " + days + (days == 1 ? " day" : " days")
                        + ", " + arrivals.length + " confirmed arrival"
                        + (arrivals.length == 1 ? "" : "s"));
        System.out.println();

        if (stranded.length == 0) {
            OutputHelper.printOK("  >> Every one of the " + arrivals.length
                    + " guests due in has a room of the type they booked.");
        } else {
            OutputHelper.printErr("  >> " + stranded.length + " of the " + arrivals.length
                    + " guests due in have no room of the type they booked.");
            OutputHelper.printBlue("     " + shortfallLine(types, wanted, ready));
        }
        System.out.println();

        String[] headers = {"ROOM TYPE", "ARRIVING", "READY", "SHORT BY"};
        Align[] aligns = {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT};
        String[][] cells = new String[types.length][];
        String[] labels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            int gap = wanted[i] - ready[i];
            labels[i] = String.valueOf(types[i]);
            cells[i] = new String[]{
                labels[i],
                String.valueOf(wanted[i]),
                String.valueOf(ready[i]),
                gap > 0 ? String.valueOf(gap) : "-"
            };
        }
        String[] table = TableRenderer.render(headers, cells, aligns);
        for (int i = 0; i < table.length; i++) {
            System.out.println("  " + table[i]);
        }
        OutputHelper.printBlue("  READY    = free and cleaned right now, ready to hand over");
        OutputHelper.printBlue("  SHORT BY = how many more of that type we would need today");

        System.out.println();
        String[] chart = ChartRenderer.groupedBars(labels, wanted, ready,
                new String[]{"Arriving", "Ready"}, "Number of rooms", "Room type", 8);
        for (int i = 0; i < chart.length; i++) {
            System.out.println("    " + chart[i]);
        }

        System.out.println();
        System.out.println("  Arrivals by tier");
        LoyaltyTier[] tiers = LoyaltyTier.values();
        int[] byTier = control.countByTier(arrivals);
        String[] tierHeaders = {"TIER", "ARRIVING", "SHARE"};
        Align[] tierAligns = {Align.LEFT, Align.RIGHT, Align.RIGHT};
        String[][] tierCells = new String[tiers.length][];
        for (int i = 0; i < tiers.length; i++) {
            tierCells[i] = new String[]{
                String.valueOf(tiers[i]),
                String.valueOf(byTier[i]),
                String.format("%.0f%%", byTier[i] * 100.0 / arrivals.length)
            };
        }
        String[] tierTable = TableRenderer.render(tierHeaders, tierCells, tierAligns);
        for (int i = 1; i < tierTable.length; i++) {
            System.out.println("    " + tierTable[i]);
        }

        if (stranded.length > 0) {
            System.out.println();
            System.out.println("  Who misses out, highest tier first:");
            printBookings(null, stranded);
            OutputHelper.printBlue("  Housekeeping can fix this by cleaning more rooms of those"
                    + " types, or the guest can be moved up to a better room if one is free.");
        }

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Arrivals : %d    Rooms ready : %d    Without a room : %d",
                arrivals.length, control.readyRoomCount(), stranded.length));
        InputHelper.waitForEnter();
    }

    /** Which types are short and which are fine, as one readable sentence. */
    private String shortfallLine(RoomType[] types, int[] wanted, int[] ready) {
        StringBuilder shortOf = new StringBuilder();
        StringBuilder fine = new StringBuilder();
        for (int i = 0; i < types.length; i++) {
            int gap = wanted[i] - ready[i];
            if (gap > 0) {
                append(shortOf, types[i] + " is " + gap + " short");
            } else {
                append(fine, String.valueOf(types[i]));
            }
        }
        if (fine.length() == 0) {
            return shortOf + ".";
        }
        return shortOf + "; " + fine + (fine.indexOf(",") < 0 ? " is fine." : " are fine.");
    }

    private void append(StringBuilder list, String item) {
        if (list.length() > 0) {
            list.append(", ");
        }
        list.append(item);
    }

    private void rangeAudit() {
        OutputHelper.printTitle("Bookings and Money by Number Range");

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
        OutputHelper.printReportHeader("Bookings and Money by Number Range",
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
        String[] shareHeaders = {"ROOM TYPE", "CHARGE", "SHARE"};
        Align[] shareAligns = {Align.LEFT, Align.RIGHT, Align.RIGHT};
        String[][] shareCells = new String[types.length][];
        String[] typeLabels = new String[types.length];
        int[] sharePercent = new int[types.length];
        for (int i = 0; i < types.length; i++) {
            double share = total == 0.0 ? 0.0 : revenue[i] * 100.0 / total;
            typeLabels[i] = String.valueOf(types[i]);
            // the chart plots whole percent: a bar cannot show a tenth anyway
            sharePercent[i] = (int) Math.round(share);
            shareCells[i] = new String[]{
                typeLabels[i],
                money(revenue[i]),
                String.format("%.1f%%", share)
            };
        }
        System.out.println("  Charge by room type");
        String[] shareRows = TableRenderer.render(shareHeaders, shareCells, shareAligns);
        for (int i = 1; i < shareRows.length; i++) {
            System.out.println("    " + shareRows[i]);
        }

        System.out.println();
        String[] chart = ChartRenderer.bars(typeLabels, sharePercent, "Share of total charge (%)", "Room type", 10);
        for (int i = 0; i < chart.length; i++) {
            System.out.println("    " + chart[i]);
        }

        printLoyaltyValue(batch, total);

        System.out.println();
        OutputHelper.printReportFooter(String.format(
                "Records : %d of %d on file    Total charge : %s    Average : %s",
                batch.length, control.size(), money(total),
                money(earning == 0 ? 0.0 : total / earning)));
        InputHelper.waitForEnter();
    }

    /**
     * What the batch is worth to the loyalty programme. The money comes off the
     * bookings, the tier off each booking's member, and the multiplier and the
     * upgrade threshold off M5's tier requirements - so the answer is not in any
     * one of the three places on its own.
     */
    private void printLoyaltyValue(Booking[] batch, double total) {
        LoyaltyTier[] tiers = LoyaltyTier.values();
        int[] bookings = control.countByTier(batch);
        int[] charge = control.chargeByTier(batch);
        int[] reward = control.rewardPointsByTier(batch);

        System.out.println();
        System.out.println("  What this earns the loyalty programme");

        String[] headers = {"TIER", "BOOKINGS", "CHARGE", "REWARD POINTS", "TIER POINTS",
            "NEXT TIER NEEDS"};
        Align[] aligns = {Align.LEFT, Align.RIGHT, Align.RIGHT, Align.RIGHT, Align.RIGHT,
            Align.RIGHT};
        String[][] cells = new String[tiers.length][];
        for (int i = 0; i < tiers.length; i++) {
            int target = control.pointsToReachNextTier(tiers[i]);
            cells[i] = new String[]{
                String.valueOf(tiers[i]),
                String.valueOf(bookings[i]),
                money(charge[i]),
                String.format("%,d", reward[i]),
                String.format("%,d", charge[i]),
                // a top tier and a threshold nobody filled in are both "no number",
                // but only one of them is a gap in M5's data
                !control.hasTierAbove(tiers[i]) ? "top tier"
                        : target < 0 ? "not set" : String.format("%,d", target)
            };
        }
        String[] table = TableRenderer.render(headers, cells, aligns);
        for (int i = 0; i < table.length; i++) {
            System.out.println("    " + table[i]);
        }
        OutputHelper.printBlue("    REWARD POINTS   = charge x that tier's multiplier,"
                + " spendable on rewards only");
        OutputHelper.printBlue("    TIER POINTS     = the charge itself - this is what moves"
                + " a member up a tier");
        OutputHelper.printBlue("    NEXT TIER NEEDS = tier points needed to reach the tier above."
                + " \"not set\" means no threshold is on file, so nobody can move up from there");

        // the finding: the multiplier makes the top tiers cost more than they spend
        int topCharge = 0;
        int topReward = 0;
        int allCharge = 0;
        int allReward = 0;
        for (int i = 0; i < tiers.length; i++) {
            allCharge += charge[i];
            allReward += reward[i];
            if (tiers[i] == LoyaltyTier.GOLD || tiers[i] == LoyaltyTier.PLATINUM) {
                topCharge += charge[i];
                topReward += reward[i];
            }
        }
        System.out.println();
        if (allReward == 0 || allCharge == 0) {
            OutputHelper.printBlue("    No reward points are earned by this batch.");
        } else {
            OutputHelper.printBlue(String.format(
                    "    Gold and Platinum are %.0f%% of the charge but %.0f%% of the reward"
                            + " points. The multiplier is where the programme's cost sits.",
                    topCharge * 100.0 / allCharge, topReward * 100.0 / allReward));
        }
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

    private String money(double amount) {
        return String.format("RM %,.2f", amount);
    }
}