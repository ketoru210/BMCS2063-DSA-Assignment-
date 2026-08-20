package entity;

import java.io.Serializable;

/**
 * Entity representing a single housekeeping task operation in Module 3.
 *
 * @author Wong Pu Jin
 */
public class HousekeepingTask implements Comparable<HousekeepingTask>, Serializable {

    private Room room;
    private String previousStatus;
    private String newStatus;

    public HousekeepingTask(Room room, String previousStatus, String newStatus) {
        this.room = room;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public Room getRoom() {
        return room;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    @Override
    public int compareTo(HousekeepingTask other) {
        if (other == null || other.getRoom() == null || this.room == null) {
            return 0;
        }
        return this.room.getRoomNo().compareToIgnoreCase(other.getRoom().getRoomNo());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HousekeepingTask that = (HousekeepingTask) obj;
        if (this.room == null || that.room == null) {
            return false;
        }
        return this.room.getRoomNo().equalsIgnoreCase(that.room.getRoomNo());
    }

    @Override
    public String toString() {
        return "Room " + room.getRoomNo() + " status changed: [" + previousStatus + "] -> [" + newStatus + "]";
    }
}