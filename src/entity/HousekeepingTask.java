package entity;

/**
 * @author Pujin
 */
public class HousekeepingTask implements Comparable<HousekeepingTask> {
    
    private Room room;
    private String previousStatus;
    private String newStatus;

    public HousekeepingTask(Room room, String previousStatus, String newStatus) {
        this.room = room;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    // --- 1. Getters (Fixes the variable warnings and Controller errors) ---
    public Room getRoom() {
        return room;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    // --- 2. compareTo (Fixes the red error on Line 3) ---
    @Override
    public int compareTo(HousekeepingTask other) {
        return this.room.getRoomNo().compareToIgnoreCase(other.getRoom().getRoomNo()); 
    }

    // --- 3. equals & toString (Required by Spec Checklist) ---
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HousekeepingTask that = (HousekeepingTask) obj;
        return this.room.getRoomNo().equalsIgnoreCase(that.getRoom().getRoomNo()); 
    }

    @Override
    public String toString() {
        return "Room " + room.getRoomNo() + " status changed: [" + previousStatus + "] -> [" + newStatus + "]";
    }
}