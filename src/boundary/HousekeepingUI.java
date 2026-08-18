package boundary;

import control.HousekeepingControl;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomType;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;
import utility.OutputHelper;
import java.util.Iterator;

/**
 * Boundary CLI for Module 3 — Housekeeping and Task Log.
 *
 * @author Pujin
 */
public class HousekeepingUI {

    private static final String TITLE = "Housekeeping and Task Log";
    private final HousekeepingControl control;
    
    /** Handed the control rather than building one: the undo/redo history has to
     *  survive leaving this screen, and M2 allocates against the same rooms. */
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
            MenuOption selected = Menu.prompt(TITLE, MenuOption.values());

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
        String divider = "+---------+----------+------------------+------------------------+";

        OutputHelper.printBlue("\n--- Current Master Room Registry ---");
        System.out.println(divider);
        System.out.printf("| %-7s | %-8s | %-16s | %-22s |%n",
                "Room No", "Type", "Occupancy", "Housekeeping Status");
        System.out.println(divider);

        for (int i = 0; i < rooms.length; i++) {
            Room r = rooms[i];
            if (r != null) {
                System.out.printf("| %-7s | %-8s | %-16s | %-22s |%n",
                        r.getRoomNo(),
                        r.getRoomType(),
                        r.getOccupancyStatus(),
                        r.getHousekeepingStatus());
            }
        }
        System.out.println(divider);

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
        System.out.println("Stack Status: [Undo Depth: " + control.getUndoCount() + "] | [Redo Depth: " + control.getRedoCount() + "]");

        String divider = "+-----+----------+-----------------------+-----------------------+";
        Iterator<HousekeepingTask> undoIt = control.getTaskLogIterator();

        if (!undoIt.hasNext()) {
            System.out.println("\n[Active Undo Stack]: Empty (No active task history)");
        } else {
            System.out.println("\n[Active Actions - Undo Stack (Most Recent First)]:");
            System.out.println(divider);
            System.out.printf("| %-3s | %-8s | %-21s | %-21s |%n", "No.", "Room No", "Previous Status", "New Status");
            System.out.println(divider);

            int index = 1;
            while (undoIt.hasNext()) {
                HousekeepingTask t = undoIt.next();
                System.out.printf("| %-3d | %-8s | %-21s | %-21s |%n",
                        index,
                        t.getRoom().getRoomNo(),
                        t.getPreviousStatus(),
                        t.getNewStatus());
                index++;
            }
            System.out.println(divider);
        }

        Iterator<HousekeepingTask> redoIt = control.getRedoLogIterator();
        if (redoIt != null && redoIt.hasNext()) {
            System.out.println("\n[Undone Actions Awaiting Redo - Redo Stack]:");
            System.out.println(divider);
            System.out.printf("| %-3s | %-8s | %-21s | %-21s |%n", "No.", "Room No", "Previous Status", "New Status");
            System.out.println(divider);

            int index = 1;
            while (redoIt.hasNext()) {
                HousekeepingTask t = redoIt.hasNext() ? redoIt.next() : null;
                if (t != null) {
                    System.out.printf("| %-3d | %-8s | %-21s | %-21s |%n",
                            index,
                            t.getRoom().getRoomNo(),
                            t.getPreviousStatus(),
                            t.getNewStatus());
                    index++;
                }
            }
            System.out.println(divider);
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

        String divider = "+---------+----------+------------------+------------------------+";
        System.out.println("\n" + divider);
        System.out.printf("| %-7s | %-8s | %-16s | %-22s |%n", "Room No", "Type", "Occupancy", "Housekeeping Status");
        System.out.println(divider);

        int readyCount = 0;
        int pendingCount = 0;
        for (int i = 0; i < filtered.length; i++) {
            Room r = filtered[i];
            System.out.printf("| %-7s | %-8s | %-16s | %-22s |%n",
                    r.getRoomNo(), r.getRoomType(), r.getOccupancyStatus(), r.getHousekeepingStatus());

            if ("Ready for Check-In".equalsIgnoreCase(r.getHousekeepingStatus())) {
                readyCount++;
            } else {
                pendingCount++;
            }
        }
        System.out.println(divider);

        int totalFiltered = filtered.length;
        double readyPct = (totalFiltered > 0) ? ((double) readyCount / totalFiltered) * 100.0 : 0.0;
        double pendingPct = (totalFiltered > 0) ? ((double) pendingCount / totalFiltered) * 100.0 : 0.0;

        System.out.println("\n--- Summary Metrics ---");
        System.out.println("Total Matching Rooms: " + totalFiltered);
        System.out.printf("Operational Ready Rate: %d room(s) (%.1f%%)%n", readyCount, readyPct);
        System.out.printf("Pending Cleaning/Inspection: %d room(s) (%.1f%%)%n", pendingCount, pendingPct);
        System.out.println("=================================================================\n");
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

        System.out.println("\n------------------------------------------------------------------");
        System.out.printf("%-6s | %-60s%n", "No.", "Action Event Details");
        System.out.println("------------------------------------------------------------------");

        if (tasks.length == 0) {
            System.out.println("No matching housekeeping task log entries found.");
        } else {
            for (int i = 0; i < tasks.length; i++) {
                System.out.printf("%-6d | %-60s%n", (i + 1), tasks[i]);
            }
        }
        System.out.println("------------------------------------------------------------------");

        System.out.println("\n--- Audit Analytics & Metrics ---");
        System.out.println("Total Filtered Task Events: " + tasks.length);
        System.out.println("Active Undo Stack Depth: " + control.getUndoCount());
        System.out.println("Active Redo Stack Depth: " + control.getRedoCount());
        System.out.println("==================================================================\n");
    }
}