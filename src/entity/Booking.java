package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * PLACEHOLDER — field contract per plan.md, replace with the owner's version but
 * keep these getter names so dao/ and M2's control keep compiling.
 * <p>
 * {@code compareTo} orders by confirmation number because M4's BST keys on it —
 * that is fixed, and it is why the allocation queue needs its own entry type
 * rather than heaping Bookings directly.
 *
 * @author YZ (placeholder)
 */
public class Booking implements Serializable, Comparable<Booking> {
    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final String confirmationNo;
    private final Member member;
    private final RoomType roomType;
    private final LocalDate checkIn;
    private final LocalDate checkOut;

    private Room room;
    private String status;

    public Booking(String confirmationNo, Member member, RoomType roomType,
                   LocalDate checkIn, LocalDate checkOut, String status) {
        this.confirmationNo = confirmationNo;
        this.member = member;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.status = status;
    }

    public String getConfirmationNo() {
        return confirmationNo;
    }

    public Member getMember() {
        return member;
    }

    /** The type the guest asked for — revenue is charged on this, not on the room actually given. */
    public RoomType getRoomType() {
        return roomType;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    /** null until M2 allocates a room. */
    public Room getRoom() {
        return room;
    }

    public String getStatus() {
        return status;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAllocated() {
        return room != null;
    }

    public long getNights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    @Override
    public int compareTo(Booking other) {
        return confirmationNo.compareTo(other.confirmationNo);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return confirmationNo.equals(((Booking) obj).confirmationNo);
    }

    @Override
    public int hashCode() {
        return confirmationNo.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s | %-18s | %-6s | %-6s | %s - %s | %s",
                confirmationNo,
                member.getName(),
                roomType,
                room == null ? "-" : room.getRoomNo(),
                checkIn.format(DATE_FORMAT),
                checkOut.format(DATE_FORMAT),
                status);
    }
}
