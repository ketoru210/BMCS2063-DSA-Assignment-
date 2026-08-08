package control;

import adt.LinkedStack;
import dao.RoomDAO;
import entity.HousekeepingTask;
import entity.Room;

/**
 * @author Pujin
 */
public class HousekeepingControl {
    
    // Changed to use the concrete LinkedStack class per Iron Rule #1
    private LinkedStack<HousekeepingTask> undoStack;
    private LinkedStack<HousekeepingTask> redoStack;
    private RoomDAO roomDAO;

    public HousekeepingControl() {
        undoStack = new LinkedStack<>();
        redoStack = new LinkedStack<>();
        roomDAO = new RoomDAO();
    }

    public Room[] getAllRooms() {
        return roomDAO.getAllRooms();
    }

    public Room findRoom(String roomNo) {
        Room[] rooms = roomDAO.getAllRooms();
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i].getRoomNo().equalsIgnoreCase(roomNo)) {
                return rooms[i];
            }
        }
        return null;
    }

    public boolean updateRoomStatus(String roomNo, String newStatus) {
        Room room = findRoom(roomNo);
        if (room == null) return false;

        HousekeepingTask task = new HousekeepingTask(room, room.getHousekeepingStatus(), newStatus);
        
        undoStack.push(task);
        redoStack.clear(); // A new action clears the redo history
        
        room.setHousekeepingStatus(newStatus);
        return true;
    }

    public boolean undoLastTask() {
        if (undoStack.isEmpty()) return false;

        HousekeepingTask lastTask = undoStack.pop();
        redoStack.push(lastTask);
        
        lastTask.getRoom().setHousekeepingStatus(lastTask.getPreviousStatus());
        return true;
    }

    public boolean redoLastTask() {
        if (redoStack.isEmpty()) return false;

        HousekeepingTask revertedTask = redoStack.pop();
        undoStack.push(revertedTask);
        
        revertedTask.getRoom().setHousekeepingStatus(revertedTask.getNewStatus());
        return true;
    }

    /**
     * Business Logic (ECB): Returns allowed status progression choices based on current status.
     */
    public String[] getValidNextStatuses(String currentStatus) {
        if (currentStatus == null) {
            return new String[]{"Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"};
        }
        switch (currentStatus) {
            case "Dirty":
                return new String[]{"Cleaning In Progress"};
            case "Cleaning In Progress":
                return new String[]{"Inspected"};
            case "Inspected":
                return new String[]{"Ready for Check-In", "Dirty"};
            case "Ready for Check-In":
                return new String[]{"Dirty"};
            default:
                return new String[]{"Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In"};
        }
    }

    /**
     * Returns an iterator over the undo stack history (invokes LinkedStack.getIterator()).
     */
    public java.util.Iterator<HousekeepingTask> getTaskLogIterator() {
        return undoStack.getIterator();
    }

    /**
     * Returns the most recent task on the undo stack without removing it (invokes LinkedStack.peek()).
     */
    public HousekeepingTask getTopTask() {
        return undoStack.peek();
    }

    public int getUndoCount() {
        return undoStack.size();
    }

    public int getRedoCount() {
        return redoStack.size();
    }
}