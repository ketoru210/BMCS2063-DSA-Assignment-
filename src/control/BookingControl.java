package control;

import dao.BookingDAO;
import entity.Booking;

/**
 * PLACEHOLDER for M4 — Front-Desk Service. Written so M2 has somewhere to read
 * bookings from; QW replaces the array with the BinarySearchTree and keeps (or
 * renames, and tells M2) the two lookup methods.
 * <p>
 * A DAO is read by its owning module's control and by nobody else — every other
 * module goes through this class. Combined with there being exactly one instance
 * of it, created in {@code Main}, that is what keeps M2 and M4 looking at one
 * set of Booking objects rather than two.
 *
 * @author YZ (placeholder for QW)
 */
public class BookingControl {

    private final Booking[] bookings;

    public BookingControl() {
        bookings = new BookingDAO().getAllBookings();
    }

    public Booking[] getAllBookings() {
        return bookings;
    }

    public Booking findByConfirmationNo(String confirmationNo) {
        if (confirmationNo == null) {
            return null;
        }
        for (int i = 0; i < bookings.length; i++) {
            if (bookings[i].getConfirmationNo().equals(confirmationNo)) {
                return bookings[i];
            }
        }
        return null;
    }
}
