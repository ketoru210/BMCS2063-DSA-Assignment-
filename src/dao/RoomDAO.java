package dao;

import adt.CollectionInterface;
import adt.DoublyLinkedList;
import entity.Room;
import entity.RoomType;

/**
 * DAO for Room entity master registry.
 * Hardcodes initial seeded rooms into team ADT DoublyLinkedList.
 *
 * @author Wong Pu Jin
 */
public class RoomDAO {
    // M2 hands out a room only when occupancy is Available AND housekeeping is
    // Ready for Check-In, so the seed deliberately carries rooms that satisfy
    // one condition but not the other.
    private static final CollectionInterface<Room> rooms = new DoublyLinkedList<>();

    static {
        rooms.add(new Room("A-101", RoomType.SINGLE, "Available", "Ready for Check-In"));
        rooms.add(new Room("A-102", RoomType.SINGLE, "Available", "Ready for Check-In"));
        rooms.add(new Room("A-103", RoomType.SINGLE, "Available", "Ready for Check-In"));
        rooms.add(new Room("A-104", RoomType.SINGLE, "Available", "Ready for Check-In"));
        rooms.add(new Room("A-105", RoomType.DELUXE, "Available", "Ready for Check-In"));
        rooms.add(new Room("B-201", RoomType.DELUXE, "Available", "Ready for Check-In"));
        rooms.add(new Room("B-202", RoomType.DELUXE, "Available", "Ready for Check-In"));
        rooms.add(new Room("B-203", RoomType.DELUXE, "Available", "Ready for Check-In"));
        rooms.add(new Room("B-204", RoomType.SUITE, "Available", "Ready for Check-In"));
        rooms.add(new Room("C-301", RoomType.SUITE, "Available", "Ready for Check-In"));
        rooms.add(new Room("C-302", RoomType.SUITE, "Available", "Ready for Check-In"));
        rooms.add(new Room("C-303", RoomType.SUITE, "Available", "Ready for Check-In"));
    }

    public RoomDAO() {
    }

    public CollectionInterface<Room> getRoomCollection() {
        return rooms;
    }

}
