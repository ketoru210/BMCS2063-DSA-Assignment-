package entity;

import java.io.Serializable;

/**
 * @author Pujin
 */
public class Room implements Serializable {
    private String roomNo;
    private String roomType;
    private String occupancyStatus;
    private String housekeepingStatus;

    public Room(String roomNo, String roomType, String occupancyStatus, String housekeepingStatus) {
        this.roomNo = roomNo;
        this.roomType = roomType;
        this.occupancyStatus = occupancyStatus;
        this.housekeepingStatus = housekeepingStatus;
    }

    // --- Getters ---
    public String getRoomNo() { return roomNo; }
    
    // NEW: Added missing getters for the UI table
    public String getRoomType() { return roomType; }
    public String getOccupancyStatus() { return occupancyStatus; }
    
    public String getHousekeepingStatus() { return housekeepingStatus; }
    
    // --- Setters ---
    public void setHousekeepingStatus(String housekeepingStatus) { 
        this.housekeepingStatus = housekeepingStatus; 
    }

    @Override
    public String toString() {
        return String.format("Room: %-6s | Type: %-6s | Occ: %-15s | HK Status: %s", 
                roomNo, roomType, occupancyStatus, housekeepingStatus);
    }
}