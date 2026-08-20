package entity;

import java.io.Serializable;

/**
 * @author Wong Pu Jin
 */
public class Room implements Serializable, Comparable<Room> {
    private String roomNo;
    private RoomType roomType;
    private String occupancyStatus;
    private String housekeepingStatus;

    public Room(String roomNo, RoomType roomType, String occupancyStatus, String housekeepingStatus) {
        this.roomNo = roomNo;
        this.roomType = roomType;
        this.occupancyStatus = occupancyStatus;
        this.housekeepingStatus = housekeepingStatus;
    }

    // --- Getters ---
    public String getRoomNo() {
        return roomNo;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getOccupancyStatus() {
        return occupancyStatus;
    }

    public String getHousekeepingStatus() {
        return housekeepingStatus;
    }
    
    // --- Setters ---
    // Available -> Reserved is M2's; Reserved -> Occupied / -> Available is M4's
    public void setOccupancyStatus(String occupancyStatus) {
        this.occupancyStatus = occupancyStatus;
    }


    public void setHousekeepingStatus(String housekeepingStatus) { 
        this.housekeepingStatus = housekeepingStatus; 
    }

    @Override
    public int compareTo(Room other) {
        return roomNo.compareToIgnoreCase(other.roomNo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return roomNo.equalsIgnoreCase(((Room) obj).roomNo);
    }

    @Override
    public String toString() {
        return String.format("Room: %-6s | Type: %-6s | Occ: %-15s | HK Status: %s",
                roomNo, roomType, occupancyStatus, housekeepingStatus);
    }
}