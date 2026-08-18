package control;

import adt.CollectionInterface;
import adt.LinkedStack;
import dao.RoomDAO;
import entity.HousekeepingTask;
import entity.Room;
import entity.RoomType;
import java.util.Iterator;

/**
 * Control for Module 3 — Housekeeping and Task Log.
 * <p>
 * Manages room housekeeping state transitions and records operation history
 * using an undo/redo stack pair backed by the team's {@code LinkedStack} ADT.
 * Supports state machine validation and custom report filtering/sorting.
 *
 * @author Pujin
 */
public class HousekeepingControl {

    private final CollectionInterface<HousekeepingTask> undoStack;
    private final CollectionInterface<HousekeepingTask> redoStack;
    private final RoomDAO roomDAO;

    public HousekeepingControl() {
        undoStack = new LinkedStack<>();
        redoStack = new LinkedStack<>();
        roomDAO = new RoomDAO();
    }

    // --- queries ---

    public Room[] getAllRooms() {
        return roomDAO.getAllRooms();
    }

    public Room findRoom(String roomNo) {
        if (roomNo == null) {
            return null;
        }
        Room[] rooms = roomDAO.getAllRooms();
        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i] != null && rooms[i].getRoomNo().equalsIgnoreCase(roomNo)) {
                return rooms[i];
            }
        }
        return null;
    }

    // --- status updates ---

    public boolean updateRoomStatus(String roomNo, String newStatus) {
        Room room = findRoom(roomNo);
        if (room == null || newStatus == null) {
            return false;
        }

        HousekeepingTask task = new HousekeepingTask(room, room.getHousekeepingStatus(), newStatus);
        undoStack.add(task);
        redoStack.clear();

        room.setHousekeepingStatus(newStatus);
        return true;
    }

    /**
     * Sets room housekeeping status to Dirty upon guest check-out.
     */
    public boolean markDirty(Room room) {
        if (room == null) {
            return false;
        }
        return updateRoomStatus(room.getRoomNo(), "Dirty");
    }

    // --- undo / redo ---

    public boolean undoLastTask() {
        if (undoStack.isEmpty()) {
            return false;
        }

        HousekeepingTask lastTask = undoStack.remove();
        redoStack.add(lastTask);

        lastTask.getRoom().setHousekeepingStatus(lastTask.getPreviousStatus());
        return true;
    }

    public boolean redoLastTask() {
        if (redoStack.isEmpty()) {
            return false;
        }

        HousekeepingTask revertedTask = redoStack.remove();
        undoStack.add(revertedTask);

        revertedTask.getRoom().setHousekeepingStatus(revertedTask.getNewStatus());
        return true;
    }

    /**
     * Returns allowed status progression choices based on current status.
     */
    public String[] getValidNextStatuses(String currentStatus) {
        if (currentStatus == null) {
            return new String[] { "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In" };
        }
        switch (currentStatus) {
            case "Dirty":
                return new String[] { "Cleaning In Progress" };
            case "Cleaning In Progress":
                return new String[] { "Inspected" };
            case "Inspected":
                return new String[] { "Ready for Check-In", "Dirty" };
            case "Ready for Check-In":
                return new String[] { "Dirty" };
            default:
                return new String[] { "Dirty", "Cleaning In Progress", "Inspected", "Ready for Check-In" };
        }
    }

    public Iterator<HousekeepingTask> getTaskLogIterator() {
        return undoStack.getIterator();
    }

    public Iterator<HousekeepingTask> getRedoLogIterator() {
        return redoStack.getIterator();
    }

    public HousekeepingTask getTopTask() {
        return undoStack.getFirst();
    }

    public int getUndoCount() {
        return undoStack.size();
    }

    public int getRedoCount() {
        return redoStack.size();
    }

    // --- report generation ---
    // Report 1: Room Readiness & Efficiency Summary
    /**
     * Filters rooms by status and type, then sorts using hand-written Insertion
     * Sort.
     */
    public Room[] getFilteredSortedRooms(String statusFilter, RoomType typeFilter, int sortChoice) {
        Room[] allRooms = getAllRooms();

        int matchCount = 0;
        for (int i = 0; i < allRooms.length; i++) {
            if (matchesRoomFilter(allRooms[i], statusFilter, typeFilter)) {
                matchCount++;
            }
        }

        Room[] filtered = new Room[matchCount];
        int index = 0;
        for (int i = 0; i < allRooms.length; i++) {
            if (matchesRoomFilter(allRooms[i], statusFilter, typeFilter)) {
                filtered[index++] = allRooms[i];
            }
        }

        // Insertion sort
        for (int i = 1; i < filtered.length; i++) {
            Room key = filtered[i];
            int j = i - 1;
            while (j >= 0 && compareRooms(filtered[j], key, sortChoice) > 0) {
                filtered[j + 1] = filtered[j];
                j--;
            }
            filtered[j + 1] = key;
        }

        return filtered;
    }

    private boolean matchesRoomFilter(Room room, String statusFilter, RoomType typeFilter) {
        if (room == null) {
            return false;
        }
        boolean matchStatus = (statusFilter == null || statusFilter.equalsIgnoreCase("ALL")
                || room.getHousekeepingStatus().equalsIgnoreCase(statusFilter));
        boolean matchType = (typeFilter == null || room.getRoomType() == typeFilter);
        return matchStatus && matchType;
    }

    private int compareRooms(Room r1, Room r2, int sortChoice) {
        if (sortChoice == 2) {
            return r2.getRoomNo().compareToIgnoreCase(r1.getRoomNo());
        } else if (sortChoice == 3) {
            return getStatusRank(r1.getHousekeepingStatus()) - getStatusRank(r2.getHousekeepingStatus());
        } else {
            return r1.getRoomNo().compareToIgnoreCase(r2.getRoomNo());
        }
    }

    private int getStatusRank(String status) {
        if (status == null) {
            return 0;
        }
        switch (status) {
            case "Dirty":
                return 1;
            case "Cleaning In Progress":
                return 2;
            case "Inspected":
                return 3;
            case "Ready for Check-In":
                return 4;
            default:
                return 5;
        }
    }

    // Report 2: Task Activity & Audit Trail Report
    /**
     * Traverses LinkedStack task log, filters by Zone, and sorts using hand-written
     * Selection Sort.
     */
    public HousekeepingTask[] getFilteredSortedTasks(String zoneFilter, int sortChoice) {
        int matchCount = 0;
        Iterator<HousekeepingTask> countIt = undoStack.getIterator();
        while (countIt.hasNext()) {
            if (matchesZone(countIt.next(), zoneFilter)) {
                matchCount++;
            }
        }

        HousekeepingTask[] tasks = new HousekeepingTask[matchCount];
        int index = 0;
        Iterator<HousekeepingTask> populateIt = undoStack.getIterator();
        while (populateIt.hasNext()) {
            HousekeepingTask task = populateIt.next();
            if (matchesZone(task, zoneFilter)) {
                tasks[index++] = task;
            }
        }

        // Selection sort (Chronological / room ordering)
        if (sortChoice == 2) {
            for (int i = 0; i < tasks.length - 1; i++) {
                int minIdx = i;
                for (int j = i + 1; j < tasks.length; j++) {
                    if (tasks[j].compareTo(tasks[minIdx]) < 0) {
                        minIdx = j;
                    }
                }
                HousekeepingTask temp = tasks[i];
                tasks[i] = tasks[minIdx];
                tasks[minIdx] = temp;
            }
        }

        return tasks;
    }

    private boolean matchesZone(HousekeepingTask task, String zoneFilter) {
        if (task == null || task.getRoom() == null) {
            return false;
        }
        if (zoneFilter == null || zoneFilter.equalsIgnoreCase("ALL")) {
            return true;
        }
        return task.getRoom().getRoomNo().toUpperCase().startsWith(zoneFilter.toUpperCase());
    }
}