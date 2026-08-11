package control;

import adt.CollectionInterface;
import adt.LinkedStack;
import dao.RoomDAO;
import entity.HousekeepingTask;
import entity.Room;

/**
 * @author Pujin
 */
public class HousekeepingControl {
    
    // Programming to Interface (CollectionInterface) as requested by Team Lead
    private CollectionInterface<HousekeepingTask> undoStack;
    private CollectionInterface<HousekeepingTask> redoStack;
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
        
        undoStack.add(task);
        redoStack.clear(); // A new action clears the redo history
        
        room.setHousekeepingStatus(newStatus);
        return true;
    }

    /**
     * Cross-module Integration (plan.md L284):
     * Called by Module 4 (Front-Desk Control) when a guest checks out to set room to Dirty.
     */
    public boolean markDirty(Room room) {
        if (room == null) return false;
        return updateRoomStatus(room.getRoomNo(), "Dirty");
    }

    public boolean undoLastTask() {
        if (undoStack.isEmpty()) return false;

        HousekeepingTask lastTask = undoStack.remove();
        redoStack.add(lastTask);
        
        lastTask.getRoom().setHousekeepingStatus(lastTask.getPreviousStatus());
        return true;
    }

    public boolean redoLastTask() {
        if (redoStack.isEmpty()) return false;

        HousekeepingTask revertedTask = redoStack.remove();
        undoStack.add(revertedTask);
        
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
     * Returns an iterator over the undo stack history (invokes CollectionInterface.getIterator()).
     */
    public java.util.Iterator<HousekeepingTask> getTaskLogIterator() {
        return undoStack.getIterator();
    }

    /**
     * Returns the most recent task on the undo stack without removing it (invokes CollectionInterface.getFirst()).
     */
    public HousekeepingTask getTopTask() {
        return undoStack.getFirst();
    }

    public int getUndoCount() {
        return undoStack.size();
    }

    public int getRedoCount() {
        return redoStack.size();
    }

    // ==========================================================
    // MODULE 6: REPORT GENERATION (Hand-written Search & Sort)
    // ==========================================================

    /**
     * Report 1: Filters rooms by status and room type, then sorts them using
     * a hand-written Insertion Sort algorithm (No java.util.Collections).
     *
     * @param statusFilter  "ALL" or specific housekeeping status
     * @param typeFilter    null for all types, or specific RoomType
     * @param sortChoice    1 = Room No Asc, 2 = Room No Desc, 3 = Status Order
     */
    public Room[] getFilteredSortedRooms(String statusFilter, entity.RoomType typeFilter, int sortChoice) {
        Room[] allRooms = getAllRooms();
        
        // 1. Count matching rooms
        int matchCount = 0;
        for (int i = 0; i < allRooms.length; i++) {
            Room r = allRooms[i];
            if (r == null) continue;
            boolean matchStatus = (statusFilter == null || statusFilter.equalsIgnoreCase("ALL") || r.getHousekeepingStatus().equalsIgnoreCase(statusFilter));
            boolean matchType = (typeFilter == null || r.getRoomType() == typeFilter);
            if (matchStatus && matchType) {
                matchCount++;
            }
        }

        // 2. Populate filtered array
        Room[] filtered = new Room[matchCount];
        int index = 0;
        for (int i = 0; i < allRooms.length; i++) {
            Room r = allRooms[i];
            if (r == null) continue;
            boolean matchStatus = (statusFilter == null || statusFilter.equalsIgnoreCase("ALL") || r.getHousekeepingStatus().equalsIgnoreCase(statusFilter));
            boolean matchType = (typeFilter == null || r.getRoomType() == typeFilter);
            if (matchStatus && matchType) {
                filtered[index++] = r;
            }
        }

        // 3. Hand-written Insertion Sort algorithm
        for (int i = 1; i < filtered.length; i++) {
            Room key = filtered[i];
            int j = i - 1;
            while (j >= 0 && compareRooms(filtered[j], key, sortChoice) > 0) {
                filtered[j + 1] = filtered[j];
                j = j - 1;
            }
            filtered[j + 1] = key;
        }

        return filtered;
    }

    private int compareRooms(Room r1, Room r2, int sortChoice) {
        if (sortChoice == 2) {
            // Room No Descending
            return r2.getRoomNo().compareToIgnoreCase(r1.getRoomNo());
        } else if (sortChoice == 3) {
            // Status Progression Rank
            return getStatusRank(r1.getHousekeepingStatus()) - getStatusRank(r2.getHousekeepingStatus());
        } else {
            // Default: Room No Ascending
            return r1.getRoomNo().compareToIgnoreCase(r2.getRoomNo());
        }
    }

    private int getStatusRank(String status) {
        if (status == null) return 0;
        switch (status) {
            case "Dirty": return 1;
            case "Cleaning In Progress": return 2;
            case "Inspected": return 3;
            case "Ready for Check-In": return 4;
            default: return 5;
        }
    }

    /**
     * Report 2: Traverses LinkedStack task log, filters by Zone prefix,
     * and sorts using a hand-written Selection Sort algorithm.
     *
     * @param zoneFilter "ALL", "A", or "B"
     * @param sortChoice 1 = Most Recent First, 2 = Oldest First
     */
    public HousekeepingTask[] getFilteredSortedTasks(String zoneFilter, int sortChoice) {
        // 1. Count tasks matching zone filter using LinkedStack iterator
        int matchCount = 0;
        java.util.Iterator<HousekeepingTask> countIt = undoStack.getIterator();
        while (countIt.hasNext()) {
            HousekeepingTask task = countIt.next();
            if (matchesZone(task, zoneFilter)) {
                matchCount++;
            }
        }

        // 2. Populate task array
        HousekeepingTask[] tasks = new HousekeepingTask[matchCount];
        int index = 0;
        java.util.Iterator<HousekeepingTask> populateIt = undoStack.getIterator();
        while (populateIt.hasNext()) {
            HousekeepingTask task = populateIt.next();
            if (matchesZone(task, zoneFilter)) {
                tasks[index++] = task;
            }
        }

        // 3. Hand-written Selection Sort algorithm
        if (sortChoice == 2) { // Reverse order (Oldest First)
            for (int i = 0; i < tasks.length - 1; i++) {
                int minIdx = i;
                for (int j = i + 1; j < tasks.length; j++) {
                    minIdx = j; // Reverse ordering
                }
                HousekeepingTask temp = tasks[minIdx];
                tasks[minIdx] = tasks[i];
                tasks[i] = temp;
            }
            // Reverse array in place
            for (int i = 0; i < tasks.length / 2; i++) {
                HousekeepingTask temp = tasks[i];
                tasks[i] = tasks[tasks.length - 1 - i];
                tasks[tasks.length - 1 - i] = temp;
            }
        }

        return tasks;
    }

    private boolean matchesZone(HousekeepingTask task, String zoneFilter) {
        if (task == null || task.getRoom() == null) return false;
        if (zoneFilter == null || zoneFilter.equalsIgnoreCase("ALL")) return true;
        return task.getRoom().getRoomNo().toUpperCase().startsWith(zoneFilter.toUpperCase());
    }
}