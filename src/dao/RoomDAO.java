package dao;

import entity.Room;
import entity.RoomType;

/**
 * @author Pujin
 * Description: Initializes hardcoded RAM data for the Master Registry.
 */
public class RoomDAO {
    // M2 hands out a room only when occupancy is Available AND housekeeping is
    // Ready for Check-In, so the seed deliberately carries rooms that satisfy
    // one condition but not the other.
    private static final Room[] rooms = new Room[]{
        new Room("A-101", RoomType.SINGLE, "Available", "Ready for Check-In"),
        new Room("A-102", RoomType.SINGLE, "Available", "Ready for Check-In"),
        new Room("A-103", RoomType.SINGLE, "Available", "Dirty"),
        new Room("A-104", RoomType.SINGLE, "Occupied", "Cleaning In Progress"),
        new Room("A-105", RoomType.DELUXE, "Available", "Ready for Check-In"),
        new Room("B-201", RoomType.DELUXE, "Available", "Inspected"),
        new Room("B-202", RoomType.DELUXE, "Occupied", "Dirty"),
        new Room("B-203", RoomType.DELUXE, "Available", "Dirty"),
        new Room("B-204", RoomType.SUITE, "Available", "Ready for Check-In"),
        new Room("C-301", RoomType.SUITE, "Available", "Dirty"),
        new Room("C-302", RoomType.SUITE, "Out-of-Service", "Inspected"),
        new Room("C-303", RoomType.SUITE, "Occupied", "Ready for Check-In")
    };

    public RoomDAO() {
        // Shared data contracts per plan.md
    }

    public Room[] getAllRooms() {
        return rooms;
    }
}