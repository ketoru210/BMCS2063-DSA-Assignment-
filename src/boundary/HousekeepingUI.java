package boundary;

import control.HousekeepingControl;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomType;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;
import utility.OutputHelper;
import utility.TableRenderer;
import java.util.Iterator;

/**
 * Boundary CLI for Module 3 — Housekeeping and Task Log.
 *
 * @author Pujin
 */
public class HousekeepingUI {

    private static final String[] LOG_HEADERS = { "No.", "Room No", "Previous Status", "New Status" };

    private static final String TITLE = "Housekeeping and Task Log";
    private final HousekeepingControl control;

    /**
     * Handed the control rather than building one: the undo/redo history has to
     * survive leaving this screen, and M2 allocates against the same rooms.
     */
    public HousekeepingUI(HousekeepingControl control) {
        this.control = control;
    }

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        VIEW("View All Rooms & Quick Actions"),
        UPDATE("Update Room Status (Pipeline State Machine)"),
        UNDO("Undo Last Status Update"),
        REDO("Redo Last Status Update"),
        LOG("View Task History Log (LinkedStack Traversal)"),
        REPORT1("Report 1: Room Readiness & Efficiency Summary"),
        REPORT2("Report 2: Task Activity & Audit Trail Report");

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
        }
    }

    public void run() {
        for (;;) {
            OutputHelper.clearScreen();
            MenuOption selected = Menu.prompt(TITLE, null, MenuOption.values());

            switch (selected) {
                case BACK:
                    return;
                case VIEW:
                    viewRooms();
                    break;
                case UPDATE:
                    updateStatusUI();
                    break;
                case UNDO:
                    undoUI();
                    break;
                case REDO:
                    redoUI();
                    break;
                case LOG:
                    viewTaskLogUI();
                    break;
                case REPORT1:
                    report1UI();
                    break;
                case REPORT2:
                    report2UI();
                    break;
            }
            InputHelper.waitForEnter();
        }
    }

    // --- screens ---

    private void viewRooms() {
        Room[] rooms = control.getAllRooms();
        OutputHelper.printBlue("\n--- Current Master Room Registry ---");

        int count = 0;
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i] != null) {
                count++;
            }
        }

        String[] headers = { "Room No", "Type", "Occupancy", "Housekeeping Status" };
        String[][] cells = new String[count][];
        int row = 0;
        for (int i = 0; i < rooms.length; i++) {
            Room r = rooms[i];
            if (r != null) {
                cells[row++] = new String[] {
                        r.getRoomNo(),
                        String.valueOf(r.getRoomType()),
                        r.getOccupancyStatus(),
                        r.getHousekeepingStatus()
                };
            }
        }

        String[] table = TableRenderer.renderBordered(headers, cells, null);
        for (int i = 0; i < table.length; i++) {
            System.out.println(table[i]);
        }

        while (true) {
            String choice = InputHelper.readLine("\nDo you want to update a room status now? (y/n) > ");

            if (choice.equalsIgnoreCase("y") || choice.equalsIgnoreCase("yes")) {
                updateStatusUI();
                break;
            } else if (choice.equalsIgnoreCase("n") || choice.equalsIgnoreCase("no")) {
                break;
            } else {
                OutputHelper.printErr("Invalid choice. Please enter 'y' or 'n'.");
            }
        }
    }

    private void updateStatusUI() {
        while (true) {
            System.out.println("\n---------------------------------------------------------");
            String roomNo = InputHelper.readLine("Enter Room No to Update (or '0' to exit, 'v' to view rooms) > ");

            if (roomNo.equalsIgnoreCase("0") || roomNo.equalsIgnoreCase("exit") || roomNo.equalsIgnoreCase("back")) {
                return;
            }

            if (roomNo.equalsIgnoreCase("v") || roomNo.equalsIgnoreCase("view")) {
                viewRooms();
                continue;
            }

            Room room = control.findRoom(roomNo);
            if (room == null) {
                OutputHelper.printErr("Room '" + roomNo + "' not found. Please try again.");
                continue;
            }

            String currentStatus = room.getHousekeepingStatus();
            printStatusPipeline(currentStatus);

            String[] validNextStatuses = control.getValidNextStatuses(currentStatus);

            System.out.println("Select New Status Step:");
            System.out.println("[0] Cancel / Go Back");
            for (int i = 0; i < validNextStatuses.length; i++) {
                System.out.println("[" + (i + 1) + "] Advance to: " + validNextStatuses[i]);
            }

            int choice = InputHelper.readInt("\nSelect Option > ");
            if (choice == 0) {
                return;
            }

            if (choice >= 1 && choice <= validNextStatuses.length) {
                String newStatus = validNextStatuses[choice - 1];
                control.updateRoomStatus(room.getRoomNo(), newStatus);
                OutputHelper.printOK("Success: Room " + room.getRoomNo() + " updated: [" + currentStatus + "] -> ["
                        + newStatus + "]");
                break;
            } else {
                OutputHelper.printErr("Invalid status choice. Please try again.");
            }
        }
    }

    private void printStatusPipeline(String currentStatus) {
        String[] stages = { "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In" };

        System.out.println("\n--- Pipeline State Diagram ---");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stages.length; i++) {
            if (stages[i].equalsIgnoreCase(currentStatus)) {
                sb.append("[").append(stages[i]).append("]");
            } else {
                sb.append("(").append(stages[i]).append(")");
            }
            if (i < stages.length - 1) {
                sb.append(" ==> ");
            }
        }
        System.out.println(sb.toString());
        System.out.println("Current Active Status: " + currentStatus + "\n");
    }

    private void undoUI() {
        if (control.undoLastTask()) {
            OutputHelper.printOK("Success: Last status update undone.");
            HousekeepingTask top = control.getTopTask();
            if (top != null) {
                System.out.println("Current Stack Top: " + top);
            }
        } else {
            OutputHelper.printErr("Nothing to undo! The undo stack is empty.");
        }
    }

    private void redoUI() {
        if (control.redoLastTask()) {
            OutputHelper.printOK("Success: Action redone.");
            HousekeepingTask top = control.getTopTask();
            if (top != null) {
                System.out.println("Current Stack Top: " + top);
            }
        } else {
            OutputHelper.printErr("Nothing to redo! The redo stack is empty.");
        }
    }

    private void viewTaskLogUI() {
        OutputHelper.printBlue("\n--- Housekeeping Action History Log ---");
        System.out.println("Stack Status: [Undo Depth: " + control.getUndoCount() + "] | [Redo Depth: "
                + control.getRedoCount() + "]");

        Iterator<HousekeepingTask> undoIt = control.getTaskLogIterator();

        if (!undoIt.hasNext()) {
            System.out.println("\n[Active Undo Stack]: Empty (No active task history)");
        } else {
            System.out.println("\n[Active Actions - Undo Stack (Most Recent First)]:");
            String[][] undoCells = new String[control.getUndoCount()][];
            int index = 0;
            while (undoIt.hasNext() && index < undoCells.length) {
                HousekeepingTask t = undoIt.next();
                undoCells[index] = new String[] {
                        String.valueOf(index + 1),
                        t.getRoom().getRoomNo(),
                        t.getPreviousStatus(),
                        t.getNewStatus()
                };
                index++;
            }
            String[] undoTable = TableRenderer.renderBordered(LOG_HEADERS, undoCells, null);
            for (int i = 0; i < undoTable.length; i++) {
                System.out.println(undoTable[i]);
            }
        }

        Iterator<HousekeepingTask> redoIt = control.getRedoLogIterator();
        if (redoIt != null && redoIt.hasNext()) {
            System.out.println("\n[Undone Actions Awaiting Redo - Redo Stack]:");
            String[][] redoCells = new String[control.getRedoCount()][];
            int index = 0;
            while (redoIt.hasNext() && index < redoCells.length) {
                HousekeepingTask t = redoIt.next();
                if (t != null) {
                    redoCells[index] = new String[] {
                            String.valueOf(index + 1),
                            t.getRoom().getRoomNo(),
                            t.getPreviousStatus(),
                            t.getNewStatus()
                    };
                    index++;
                }
            }
            String[] redoTable = TableRenderer.renderBordered(LOG_HEADERS, redoCells, null);
            for (int i = 0; i < redoTable.length; i++) {
                System.out.println(redoTable[i]);
            }
        }
        System.out.println();
    }

    // --- reports ---

    private void report1UI() {
        OutputHelper.printBlue("\n=== Report 1: Room Readiness & Housekeeping Efficiency Summary ===");

        System.out.println("\n[Select Housekeeping Status Criteria]");
        System.out.println("[0] All Statuses");
        System.out.println("[1] Dirty");
        System.out.println("[2] Cleaning In Progress");
        System.out.println("[3] Inspected");
        System.out.println("[4] Ready for Check-In");
        int statusChoice = InputHelper.readInt("Select Status Criteria > ");

        String statusFilter = "ALL";
        if (statusChoice == 1)
            statusFilter = "Dirty";
        else if (statusChoice == 2)
            statusFilter = "Cleaning In Progress";
        else if (statusChoice == 3)
            statusFilter = "Inspected";
        else if (statusChoice == 4)
            statusFilter = "Ready for Check-In";

        System.out.println("\n[Select Room Type Criteria]");
        System.out.println("[0] All Types");
        System.out.println("[1] SINGLE");
        System.out.println("[2] DELUXE");
        System.out.println("[3] SUITE");
        int typeChoice = InputHelper.readInt("Select Room Type Criteria > ");

        RoomType typeFilter = null;
        if (typeChoice == 1)
            typeFilter = RoomType.SINGLE;
        else if (typeChoice == 2)
            typeFilter = RoomType.DELUXE;
        else if (typeChoice == 3)
            typeFilter = RoomType.SUITE;

        System.out.println("\n[Select Sort Ordering]");
        System.out.println("[1] Room No (Ascending)");
        System.out.println("[2] Room No (Descending)");
        System.out.println("[3] Status Progression Order");
        int sortChoice = InputHelper.readInt("Select Sort Ordering > ");

        Room[] filtered = control.getFilteredSortedRooms(statusFilter, typeFilter, sortChoice);

        int dirtyCount = 0;
        int cleaningCount = 0;
        int inspectedCount = 0;
        int readyCount = 0;

        String[] headers = { "Room No", "Type", "Occupancy", "Housekeeping Status" };
        String[][] cells = new String[filtered.length][];
        for (int i = 0; i < filtered.length; i++) {
            Room r = filtered[i];
            cells[i] = new String[] {
                    r.getRoomNo(),
                    String.valueOf(r.getRoomType()),
                    r.getOccupancyStatus(),
                    r.getHousekeepingStatus()
            };

            String status = r.getHousekeepingStatus();
            if ("Dirty".equalsIgnoreCase(status)) {
                dirtyCount++;
            } else if ("Cleaning In Progress".equalsIgnoreCase(status)) {
                cleaningCount++;
            } else if ("Inspected".equalsIgnoreCase(status)) {
                inspectedCount++;
            } else if ("Ready for Check-In".equalsIgnoreCase(status)) {
                readyCount++;
            }
        }

        int pendingCount = dirtyCount + cleaningCount + inspectedCount;

        String[] table = TableRenderer.renderBordered(headers, cells, null);
        System.out.println();
        for (int i = 0; i < table.length; i++) {
            System.out.println(table[i]);
        }

        int totalFiltered = filtered.length;
        double readyPct = (totalFiltered > 0) ? ((double) readyCount / totalFiltered) * 100.0 : 0.0;
        double pendingPct = (totalFiltered > 0) ? ((double) pendingCount / totalFiltered) * 100.0 : 0.0;

        System.out.println("\n--- Summary Metrics ---");
        System.out.println("Total Matching Rooms: " + totalFiltered);
        System.out.printf("Operational Ready Rate: %d room(s) (%.1f%%)  %s%n", readyCount, readyPct,
                renderProgressBar(readyPct));
        System.out.printf("Pending Cleaning/Inspection: %d room(s) (%.1f%%)  %s%n", pendingCount, pendingPct,
                renderProgressBar(pendingPct));

        // Graphical Vertical Histogram Representation (Teacher Requirement)
        renderReport1Graph(dirtyCount, cleaningCount, inspectedCount, readyCount);
    }

    private void report2UI() {
        OutputHelper.printBlue("\n=== Report 2: Housekeeping Task Activity & Audit Trail Report ===");

        System.out.println("\n[Select Zone / Room Prefix Criteria]");
        System.out.println("[0] All Zones");
        System.out.println("[1] Zone A (A-xxx)");
        System.out.println("[2] Zone B (B-xxx)");
        System.out.println("[3] Zone C (C-xxx)");
        int zoneChoice = InputHelper.readInt("Select Zone Criteria > ");
        String zoneFilter = "ALL";
        if (zoneChoice == 1)
            zoneFilter = "A";
        else if (zoneChoice == 2)
            zoneFilter = "B";
        else if (zoneChoice == 3)
            zoneFilter = "C";

        System.out.println("\n[Select Sort Ordering]");
        System.out.println("[1] Most Recent First (LIFO Stack Order)");
        System.out.println("[2] Chronological (Oldest First)");
        int sortChoice = InputHelper.readInt("Select Sort Ordering > ");

        HousekeepingTask[] tasks = control.getFilteredSortedTasks(zoneFilter, sortChoice);

        String[] logHeaders = { "No.", "Action Event Details" };
        if (tasks.length == 0) {
            System.out.println();
            System.out.println("No matching housekeeping task log entries found.");
        } else {
            String[][] logCells = new String[tasks.length][];
            for (int i = 0; i < tasks.length; i++) {
                logCells[i] = new String[] { String.valueOf(i + 1), String.valueOf(tasks[i]) };
            }
            String[] logTable = TableRenderer.renderBordered(logHeaders, logCells, null);
            System.out.println();
            for (int i = 0; i < logTable.length; i++) {
                System.out.println(logTable[i]);
            }
        }

        System.out.println("\n--- Audit Analytics & Metrics ---");
        System.out.println("Total Filtered Task Events: " + tasks.length);
        System.out.println("Active Undo Stack Depth: " + control.getUndoCount());
        System.out.println("Active Redo Stack Depth: " + control.getRedoCount());

        // Graphical Vertical Histogram Representation (Teacher Requirement)
        renderReport2Graph(tasks);
    }

    private String renderProgressBar(double percent) {
        int width = 20;
        int filled = (int) Math.round((percent / 100.0) * width);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            if (i < filled)
                sb.append("#");
            else
                sb.append("-");
        }
        sb.append("]");
        return sb.toString();
    }

    private void renderReport1Graph(int dirty, int clean, int insp, int ready) {
        System.out.println("\n" + "=".repeat(78));
        OutputHelper.printBlue("      Report 1 Graph : ROOM READINESS & EFFICIENCY SUMMARY");
        System.out.println("=".repeat(78));

        int maxVal = Math.max(5, Math.max(dirty, Math.max(clean, Math.max(insp, ready))));
        if (maxVal < 10)
            maxVal = 10;

        System.out.println("   Room Count");
        System.out.println("   ^");
        for (int level = maxVal; level >= 1; level--) {
            System.out.printf("%2d |    %s                %s                   %s                 %s%n",
                    level,
                    dirty >= level ? "*" : " ",
                    clean >= level ? "*" : " ",
                    insp >= level ? "*" : " ",
                    ready >= level ? "*" : " ");
        }

        System.out.println("----+--------------------------------------------------------------------> Status");
        System.out.println("      Dirty     Cleaning In Progress     Inspected     Ready For Check-In");
        System.out.println("=".repeat(78));
    }

    private void renderReport2Graph(HousekeepingTask[] tasks) {
        int countA = 0, countB = 0, countC = 0;
        for (int i = 0; i < tasks.length; i++) {
            if (tasks[i] != null && tasks[i].getRoom() != null) {
                String roomNo = tasks[i].getRoom().getRoomNo().toUpperCase();
                if (roomNo.startsWith("A"))
                    countA++;
                else if (roomNo.startsWith("B"))
                    countB++;
                else if (roomNo.startsWith("C"))
                    countC++;
            }
        }

        System.out.println("\n" + "=".repeat(60));
        OutputHelper.printBlue("      Report 2 Graph : TASK ACTIVITY & AUDIT TRAIL");
        System.out.println("=".repeat(60));

        int maxVal = Math.max(5, Math.max(countA, Math.max(countB, countC)));
        if (maxVal < 10)
            maxVal = 10;

        System.out.println("   Task Count");
        System.out.println("   ^");
        for (int level = maxVal; level >= 1; level--) {
            System.out.printf("%2d |      %s             %s             %s%n",
                    level,
                    countA >= level ? "*" : " ",
                    countB >= level ? "*" : " ",
                    countC >= level ? "*" : " ");
        }

        System.out.println("----+-----------------------------------------------> Zone");
        System.out.println("        Zone A        Zone B        Zone C");
        System.out.println("=".repeat(60));
    }
}