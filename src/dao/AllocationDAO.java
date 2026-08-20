package dao;

import adt.CollectionInterface;
import adt.MaxHeap;
import entity.Allocation;
import entity.Booking;
import entity.SpecialCategory;

/**
 * Hard-coded allocation requests waiting for a room.
 * <p>
 * Arrival minutes are pre-aged across roughly 90 simulated minutes on purpose:
 * seeded at minute zero every entry would sort by tier alone and none of the
 * waiting-time crossovers would be visible during a demo. Only bookings that
 * are Confirmed and still unallocated appear here.
 * <p>
 * The bookings are handed in rather than read from BookingDAO — that DAO belongs
 * to M4, so M2 reaches its data through M4's control. The seed therefore attaches
 * to the caller's Booking objects instead of building a second copy of them.
 * <p>
 * The sort key is deliberately absent — the control computes and stores it
 * before each entry goes into the heap. The collection handed back is therefore
 * ordered on an unset key and is a transport container, nothing more: the
 * control re-keys every entry and rebuilds the heap from the shape alone, so
 * whatever order this one happens to be in is discarded.
 *
 * @author Lam Yong Zhe
 */
public class AllocationDAO {
    /** The simulated "now" the seed was aged against. */
    private static final int SEED_NOW_MINUTE = 90;

    public static int getSeedNowMinute() {
        return SEED_NOW_MINUTE;
    }

    /**
     * The seeded requests. A confirmation number missing from the pool is
     * skipped rather than seeded as null.
     * <p>
     * The pool stays a plain array: those Booking objects belong to M4 and
     * arrive through its control, so M2 does not get to choose their container.
     */
    public CollectionInterface<Allocation> getAllRequests(Booking[] pool) {
        CollectionInterface<Allocation> requests = new MaxHeap<>();

        // three Confirmed bookings are deliberately left out so that adding a
        // request has candidates from the first screen, without serving first
        requests.add(request(pool, "10042087", SpecialCategory.NONE, 0, 1));
        requests.add(request(pool, "47318206", SpecialCategory.NONE, 35, 2));
        requests.add(request(pool, "22905613", SpecialCategory.NONE, 20, 3));
        requests.add(request(pool, "68140379", SpecialCategory.NONE, 78, 4));
        requests.add(request(pool, "35672941", SpecialCategory.HABITABILITY, 55, 5));
        requests.add(request(pool, "81203756", SpecialCategory.NONE, 88, 6));
        requests.add(request(pool, "26718493", SpecialCategory.NONE, 8, 7));
        requests.add(request(pool, "50937164", SpecialCategory.LIFE_SAFETY, 62, 8));
        requests.add(request(pool, "18265037", SpecialCategory.NONE, 44, 9));

        return requests;
    }

    private static Allocation request(Booking[] pool, String confirmationNo,
                                      SpecialCategory category, int arrivalMinute, int entryNo) {
        for (Booking booking : pool) {
            if (booking.getConfirmationNo().equals(confirmationNo)) {
                return new Allocation(booking, category, arrivalMinute, entryNo);
            }
        }
        return null;
    }
}
