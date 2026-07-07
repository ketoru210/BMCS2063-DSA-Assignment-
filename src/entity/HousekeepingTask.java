package entity;

import java.io.Serializable;

/**
 * @author Pujin
 * Description: Stores a state-change record for Undo/Redo operations.
 */
public class HousekeepingTask implements Serializable {
    private Room room;
    private String previousStatus;
    private String newStatus;

    public HousekeepingTask(Room room, String previousStatus, String newStatus) {
        this.room = room;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public Room getRoom() { return room; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus() { return newStatus; }
}