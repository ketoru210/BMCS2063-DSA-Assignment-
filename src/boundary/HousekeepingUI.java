package boundary;

import control.HousekeepingControl;
import entity.Room;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;
import utility.OutputHelper;

/**
 * @author Pujin
 * Boundary: CLI for Housekeeping and Task Log (Module 3 - Stack).
 */
public class HousekeepingUI {
    
    private static final String TITLE = "Housekeeping and Task Log";
    private final HousekeepingControl control = new HousekeepingControl();

    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        VIEW("View All Rooms"),
        UPDATE("Update Room Status"),
        UNDO("Undo Last Status Update"),
        REDO("Redo Last Status Update");

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
            // Execution handled by switch block in UI run()
        }
    }

    public void run() {
        for (;;) {
            MenuOption selected = Menu.prompt(TITLE, MenuOption.values());

            if (selected == MenuOption.BACK) return;

            switch (selected) {
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
            }
            // Pause before the menu clears the screen again
            InputHelper.waitForEnter();
        }
    }

    private void viewRooms() {
        Room[] rooms = control.getAllRooms();
        String divider = "+---------+----------+------------------+------------------------+";
        
        OutputHelper.printBlue("\n--- Current Room List ---");
        System.out.println(divider);
        // Table Header
        System.out.printf("| %-7s | %-8s | %-16s | %-22s |%n", 
                "Room No", "Type", "Occupancy", "Housekeeping Status");
        System.out.println(divider);
        
        // Table Data
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
        System.out.println(divider + "\n");
    }

    private void updateStatusUI() {
        String roomNo = InputHelper.readLine("Enter Room No to Update (e.g., A-101): ");

        Room room = control.findRoom(roomNo);
        if (room == null) {
            OutputHelper.printErr("Error: Room not found.");
            return;
        }

        String currentStatus = room.getHousekeepingStatus();
        System.out.println("\nCurrent Status: " + currentStatus);
        
        // State Machine Business Logic: Enforce strict valid sequential progress
        String[] validNextStatuses;
        switch (currentStatus) {
            case "Dirty":
                validNextStatuses = new String[]{"Cleaning In Progress"};
                break;
            case "Cleaning In Progress":
                validNextStatuses = new String[]{"Inspected"};
                break;
            case "Inspected":
                // FIX APPLIED HERE: Allow the room to pass (Ready) or fail (Dirty) inspection
                validNextStatuses = new String[]{"Ready for Check-In", "Dirty"};
                break;
            case "Ready for Check-In":
                validNextStatuses = new String[]{"Dirty"};
                break;
            default:
                validNextStatuses = new String[]{"Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"};
                break;
        }

        System.out.println("Select New Status:");
        
        for (int i = 0; i < validNextStatuses.length; i++) {
            System.out.println("[" + (i + 1) + "] " + validNextStatuses[i]);
        }
        
        int choice = InputHelper.readInt("\nSelect Status > ");
        
        if (choice >= 1 && choice <= validNextStatuses.length) {
            String newStatus = validNextStatuses[choice - 1];
            control.updateRoomStatus(roomNo, newStatus);
            OutputHelper.printOK("Success: Room " + roomNo + " updated to " + newStatus);
        } else {
            OutputHelper.printErr("Invalid status choice.");
        }
    }

    private void undoUI() {
        if (control.undoLastTask()) {
            OutputHelper.printOK("Success: Last action undone instantly.");
        } else {
            OutputHelper.printErr("Nothing to undo! The schedule is fully rolled back.");
        }
    }

    private void redoUI() {
        if (control.redoLastTask()) {
            OutputHelper.printOK("Success: Action redone.");
        } else {
            OutputHelper.printErr("Nothing to redo!");
        }
    }
}