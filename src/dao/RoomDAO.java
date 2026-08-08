package dao;

import entity.Room;
import entity.RoomType;

/**
 * @author Pujin
 * Description: Initializes hardcoded RAM data for the Master Registry.
 */
public class RoomDAO {
    private static final Room[] rooms = new Room[]{
        new Room("A-101", RoomType.SINGLE, "Available", "Dirty"),
        new Room("A-102", RoomType.DELUXE, "Occupied", "Cleaning In Progress"),
        new Room("B-201", RoomType.SUITE, "Out-of-Service", "Inspected"),
        new Room("B-202", RoomType.SUITE, "Available", "Ready for Check-In")
    };

    public RoomDAO() {
        // Shared data contracts per plan.md
    }

    public Room[] getAllRooms() {
        return rooms;
    }
}