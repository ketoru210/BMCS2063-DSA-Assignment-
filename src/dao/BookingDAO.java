package dao;

import adt.CollectionInterface;
import adt.DoublyLinkedList;
import entity.Booking;
import entity.Member;
import entity.RoomType;
import java.time.LocalDate;
import java.util.Iterator;

/**
 * Hard-coded bookings (M1 was dropped; the spec permits seeding entity values
 * to RAM). Confirmation numbers are scattered on purpose — sequential keys
 * would degenerate M4's BST into a linked list.
 * <p>
 * The container handed back is a transport container, nothing more: M4 rebuilds
 * its own tree from it. A linked list rather than a tree for exactly that
 * reason — a tree would hand the numbers over in ascending order and the tree
 * built from them would be the right spine the scattering was meant to avoid.
 *
 * @author YZ
 */
public class BookingDAO {
    // static so every control seeded from here shares one set of objects
    private static final CollectionInterface<Booking> BOOKINGS = seed();

    private static CollectionInterface<Booking> seed() {
        MemberDAO members = new MemberDAO();
        CollectionInterface<Booking> bookings = new DoublyLinkedList<>();

        // no booking carries a room: nothing has been allocated when the app starts
        bookings.add(booking("10042087", members.findByUsername("ravic"), RoomType.SINGLE, 21, 24, "Confirmed"));
        bookings.add(booking("47318206", members.findByUsername("limkx"), RoomType.DELUXE, 21, 23, "Confirmed"));
        bookings.add(booking("22905613", members.findByUsername("sitiz"), RoomType.SINGLE, 22, 26, "Confirmed"));
        bookings.add(booking("68140379", members.findByUsername("tanwm"), RoomType.SUITE, 21, 25, "Confirmed"));
        bookings.add(booking("35672941", members.findByUsername("chongml"), RoomType.DELUXE, 21, 22, "Confirmed"));
        bookings.add(booking("81203756", members.findByUsername("arunk"), RoomType.SINGLE, 23, 25, "Confirmed"));
        bookings.add(booking("26718493", members.findByUsername("wongsh"), RoomType.SUITE, 21, 24, "Confirmed"));
        bookings.add(booking("50937164", members.findByUsername("rajesh"), RoomType.DELUXE, 22, 25, "Confirmed"));
        bookings.add(booking("18265037", members.findByUsername("gohyl"), RoomType.SINGLE, 21, 23, "Confirmed"));
        bookings.add(booking("71549826", members.findByUsername("faridah"), RoomType.SINGLE, 22, 24, "Confirmed"));
        bookings.add(booking("39084517", members.findByUsername("leejh"), RoomType.DELUXE, 23, 26, "Confirmed"));
        bookings.add(booking("85216740", members.findByUsername("nurula"), RoomType.SINGLE, 24, 27, "Confirmed"));
        bookings.add(booking("59418032", members.findByUsername("nurula"), RoomType.SUITE, 28, 30, "Pending"));
        bookings.add(booking("43790281", members.findByUsername("rajesh"), RoomType.SUITE, 25, 28, "Pending"));
        bookings.add(booking("74035189", members.findByUsername("tanwm"), RoomType.DELUXE, 15, 17, "Cancelled"));
        bookings.add(booking("62158904", members.findByUsername("gohyl"), RoomType.DELUXE, 20, 22, "Cancelled"));

        return bookings;
    }

    private static Booking booking(String confirmationNo, Member member, RoomType roomType,
                                   int checkInDay, int checkOutDay, String status) {
        return new Booking(confirmationNo, member, roomType,
                LocalDate.of(2026, 8, checkInDay),
                LocalDate.of(2026, 8, checkOutDay),
                status);
    }

    public CollectionInterface<Booking> getAllBookings() {
        return BOOKINGS;
    }

    public Booking findByConfirmationNo(String confirmationNo) {
        Iterator<Booking> walker = BOOKINGS.getIterator();
        while (walker.hasNext()) {
            Booking booking = walker.next();
            if (booking.getConfirmationNo().equals(confirmationNo)) {
                return booking;
            }
        }
        return null;
    }
}
