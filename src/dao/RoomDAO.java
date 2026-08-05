package dao;

import entity.Room;

/**
 * @author Pujin
 * Description: Initializes hardcoded RAM data for the Master Registry.
 */
public class RoomDAO {
    private static final Room[] rooms = new Room[]{
        new Room("A-101", "Single", "Available", "Dirty"),
        new Room("A-102", "Deluxe", "Occupied", "Cleaning In Progress"),
        new Room("B-201", "Suite", "Out-of-Service", "Inspected"),
        new Room("B-202", "Suite", "Available", "Ready for Check-In")
    };

    public RoomDAO() {
        // Shared data contracts per plan.md
    }

    public Room[] getAllRooms() {
        return rooms;
    }
}