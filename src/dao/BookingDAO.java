package dao;

import entity.Booking;
import entity.Member;
import entity.RoomType;
import java.time.LocalDate;

/**
 * Hard-coded bookings (M1 was dropped; the spec permits seeding entity values
 * to RAM). Confirmation numbers are scattered on purpose — sequential keys
 * would degenerate M4's BST into a linked list.
 *
 * @author YZ
 */
public class BookingDAO {
    // static so every control seeded from here shares one set of objects
    private static final Booking[] BOOKINGS = seed();

    private static Booking[] seed() {
        MemberDAO members = new MemberDAO();
        // no booking carries a room: nothing has been allocated when the app starts
        return new Booking[]{
            booking("10042087", members.findByUsername("ravic"), RoomType.SINGLE, 21, 24, "Confirmed"),
            booking("47318206", members.findByUsername("limkx"), RoomType.DELUXE, 21, 23, "Confirmed"),
            booking("22905613", members.findByUsername("sitiz"), RoomType.SINGLE, 22, 26, "Confirmed"),
            booking("68140379", members.findByUsername("tanwm"), RoomType.SUITE, 21, 25, "Confirmed"),
            booking("35672941", members.findByUsername("chongml"), RoomType.DELUXE, 21, 22, "Confirmed"),
            booking("81203756", members.findByUsername("arunk"), RoomType.SINGLE, 23, 25, "Confirmed"),
            booking("26718493", members.findByUsername("wongsh"), RoomType.SUITE, 21, 24, "Confirmed"),
            booking("50937164", members.findByUsername("rajesh"), RoomType.DELUXE, 22, 25, "Confirmed"),
            booking("18265037", members.findByUsername("gohyl"), RoomType.SINGLE, 21, 23, "Confirmed"),
            booking("71549826", members.findByUsername("faridah"), RoomType.SINGLE, 22, 24, "Confirmed"),
            booking("39084517", members.findByUsername("leejh"), RoomType.DELUXE, 23, 26, "Confirmed"),
            booking("85216740", members.findByUsername("nurula"), RoomType.SINGLE, 24, 27, "Confirmed"),
            booking("59418032", members.findByUsername("nurula"), RoomType.SUITE, 28, 30, "Pending"),
            booking("43790281", members.findByUsername("rajesh"), RoomType.SUITE, 25, 28, "Pending"),
            booking("74035189", members.findByUsername("tanwm"), RoomType.DELUXE, 15, 17, "Cancelled"),
            booking("62158904", members.findByUsername("gohyl"), RoomType.DELUXE, 20, 22, "Cancelled")
        };
    }

    private static Booking booking(String confirmationNo, Member member, RoomType roomType,
                                   int checkInDay, int checkOutDay, String status) {
        return new Booking(confirmationNo, member, roomType,
                LocalDate.of(2026, 8, checkInDay),
                LocalDate.of(2026, 8, checkOutDay),
                status);
    }

    public Booking[] getAllBookings() {
        return BOOKINGS;
    }

    public Booking findByConfirmationNo(String confirmationNo) {
        for (int i = 0; i < BOOKINGS.length; i++) {
            if (BOOKINGS[i].getConfirmationNo().equals(confirmationNo)) {
                return BOOKINGS[i];
            }
        }
        return null;
    }
}
