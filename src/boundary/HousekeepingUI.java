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
        OutputHelper.printBlue("\n--- Current Room List ---");
        Room[] rooms = control.getAllRooms();
        for (int i = 0; i < rooms.length; i++) {
            System.out.println(rooms[i].toString());
        }
    }

    private void updateStatusUI() {
        String roomNo = InputHelper.readLine("Enter Room No to Update (e.g., A-101): ");

        Room room = control.findRoom(roomNo);
        if (room == null) {
            OutputHelper.printErr("Error: Room not found.");
            return;
        }

        System.out.println("\nCurrent Status: " + room.getHousekeepingStatus());
        System.out.println("Select New Status:");
        
        String[] statuses = {
            "Dirty", 
            "Cleaning In Progress", 
            "Inspected", 
            "Ready for Check-In"
        };
        
        // Print options 1 to 4 to avoid 0 index confusion for statuses
        for (int i = 0; i < statuses.length; i++) {
            System.out.println("[" + (i + 1) + "] " + statuses[i]);
        }
        
        int choice = InputHelper.readInt("\nSelect Status > ");
        
        if (choice >= 1 && choice <= statuses.length) {
            String newStatus = statuses[choice - 1];
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