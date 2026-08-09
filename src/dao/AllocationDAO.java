package dao;

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
 * before each entry goes into the heap.
 *
 * @author YZ
 */
public class AllocationDAO {
    /** The simulated "now" the seed was aged against. */
    private static final int SEED_NOW_MINUTE = 90;

    public static int getSeedNowMinute() {
        return SEED_NOW_MINUTE;
    }

    /** Entries whose confirmation number is missing from the pool come back null. */
    public Allocation[] getAllRequests(Booking[] pool) {
        return new Allocation[]{
            request(pool, "10042087", SpecialCategory.NONE, 0, 1),
            request(pool, "47318206", SpecialCategory.NONE, 35, 2),
            request(pool, "22905613", SpecialCategory.NONE, 20, 3),
            request(pool, "68140379", SpecialCategory.NONE, 78, 4),
            request(pool, "35672941", SpecialCategory.HABITABILITY, 55, 5),
            request(pool, "81203756", SpecialCategory.NONE, 88, 6)
        };
    }

    private static Allocation request(Booking[] pool, String confirmationNo,
                                      SpecialCategory category, int arrivalMinute, int entryNo) {
        for (int i = 0; i < pool.length; i++) {
            if (pool[i].getConfirmationNo().equals(confirmationNo)) {
                return new Allocation(pool[i], category, arrivalMinute, entryNo);
            }
        }
        return null;
    }
}
