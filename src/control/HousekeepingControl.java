package control;

import adt.LinkedStack; // StackInterface import is removed
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
}