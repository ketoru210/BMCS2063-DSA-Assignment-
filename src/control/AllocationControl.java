package control;

import adt.MaxHeap;
import dao.AllocationDAO;
import entity.Allocation;
import entity.Booking;
import entity.SpecialCategory;
import java.util.Iterator;

/**
 * Control for M2 — VIP &amp; Loyalty Tier-Priority Room Allocation.
 * <p>
 * Owns the waiting queue and the simulated clock. The sort key is computed here
 * and stored on the entry before it enters the heap, so the heap itself never
 * reads a clock and never has to be rebuilt as simulated time advances.
 * <p>
 * The clock is simulated rather than real: against a wall clock every seeded
 * request would arrive at minute zero and none of the tier-versus-waiting
 * crossovers would ever be visible.
 * <p>
 * Bookings are read through M4's control, never through {@code BookingDAO}: a
 * DAO is private to its own module, and going through the control is what keeps
 * both modules pointing at one set of Booking objects.
 *
 * @author YZ
 */
public class AllocationControl {
    // only Confirmed bookings queue for a room; move to utility/ when M4 needs it too
    private static final String STATUS_CONFIRMED = "Confirmed";

    // concrete type on purpose: the heap's add-on methods stay reachable
    private final MaxHeap<Allocation> queue = new MaxHeap<>();

    private final BookingControl bookingControl;

    private int nextEntryNo = 1;
    private int clockMinute;

    public AllocationControl(BookingControl bookingControl) {
        this.bookingControl = bookingControl;
        clockMinute = AllocationDAO.getSeedNowMinute();

        Allocation[] seeded = new AllocationDAO().getAllRequests(bookingControl.getAllBookings());
        for (int i = 0; i < seeded.length; i++) {
            if (seeded[i] == null) {
                continue;
            }
            admit(seeded[i]);
            if (seeded[i].getEntryNo() >= nextEntryNo) {
                nextEntryNo = seeded[i].getEntryNo() + 1;
            }
        }
    }

    // --- simulated clock ---

    public int getClockMinute() {
        return clockMinute;
    }

    public void fastForward(int minutes) {
        if (minutes > 0) {
            clockMinute += minutes;
        }
    }

    // --- create ---

    /**
     * Puts a booking into the queue at the current simulated minute.
     * Returns the entry, or null if the booking cannot join.
     */
    public Allocation enqueue(Booking booking, SpecialCategory category) {
        if (booking == null || category == null || booking.isAllocated()) {
            return null;
        }
        // goes through the collection: the same booking must not queue twice
        if (findByConfirmationNo(booking.getConfirmationNo()) != null) {
            return null;
        }

        Allocation entry = new Allocation(booking, category, clockMinute, nextEntryNo);
        if (!admit(entry)) {
            return null;
        }
        nextEntryNo++;
        return entry;
    }

    // --- read ---

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /** Highest priority entry, left in the queue. */
    public Allocation peekNext() {
        return queue.getFirst();
    }

    /** Lowest priority entry — the one to watch when checking for starvation. */
    public Allocation peekLast() {
        return queue.getLast();
    }

    /** Level order, which for this heap is array order — NOT priority order. */
    public Allocation[] getLevelOrder() {
        Allocation[] entries = new Allocation[queue.size()];
        Iterator<Allocation> walker = queue.getIterator();
        int i = 0;
        while (walker.hasNext()) {
            entries[i] = walker.next();
            i++;
        }
        return entries;
    }

    /**
     * The queue in the order it will actually be served, highest priority first.
     * <p>
     * TEMPORARY — this selection sort is a stand-in so the UI has something to
     * render. It is also the hand-written sort the report is graded on, so
     * replace it with your own, or move it into MaxHeap as a toSortedArray()
     * add-on and call that instead.
     */
    public Allocation[] getServeOrder() {
        Allocation[] entries = getLevelOrder();
        for (int i = 0; i < entries.length - 1; i++) {
            int best = i;
            for (int j = i + 1; j < entries.length; j++) {
                if (entries[j].compareTo(entries[best]) > 0) {
                    best = j;
                }
            }
            Allocation temp = entries[i];
            entries[i] = entries[best];
            entries[best] = temp;
        }
        return entries;
    }

    /** Confirmed bookings that have no room yet and are not already waiting. */
    public Booking[] getQueueableBookings() {
        Booking[] all = bookingControl.getAllBookings();

        int count = 0;
        for (int i = 0; i < all.length; i++) {
            if (isQueueable(all[i])) {
                count++;
            }
        }

        Booking[] queueable = new Booking[count];
        int filled = 0;
        for (int i = 0; i < all.length; i++) {
            if (isQueueable(all[i])) {
                queueable[filled] = all[i];
                filled++;
            }
        }
        return queueable;
    }

    private boolean isQueueable(Booking booking) {
        return STATUS_CONFIRMED.equals(booking.getStatus())
                && !booking.isAllocated()
                && findByConfirmationNo(booking.getConfirmationNo()) == null;
    }

    // search(probe) needs a probe carrying the whole sort key, and a confirmation
    // number alone cannot build one — so this lookup walks instead
    public Allocation findByConfirmationNo(String confirmationNo) {
        if (confirmationNo == null) {
            return null;
        }
        Iterator<Allocation> walker = queue.getIterator();
        while (walker.hasNext()) {
            Allocation entry = walker.next();
            if (entry.getBooking().getConfirmationNo().equals(confirmationNo)) {
                return entry;
            }
        }
        return null;
    }

    /** Score to show the user: always positive, and ranks exactly like the stored key. */
    public long livePriority(Allocation entry) {
        return entry.getInvariantPriority() + clockMinute;
    }

    // --- delete ---

    /** Removes and returns the highest priority entry; null when the queue is empty. */
    public Allocation serveNext() {
        return queue.remove();
    }

    public boolean cancel(Allocation entry) {
        return entry != null && queue.remove(entry);
    }

    public void clearQueue() {
        queue.clear();
    }

    // --- priority ---

    private boolean admit(Allocation entry) {
        entry.setInvariantPriority(computeInvariantPriority(entry));
        return queue.add(entry);
    }

    /**
     * Tier weight minus arrival minute — the priority the entry would have at
     * minute zero. The live score adds the same clock reading to everybody, so
     * dropping it changes no ordering and the stored key never goes stale.
     */
    private long computeInvariantPriority(Allocation entry) {
        return entry.getTier().getWeight() - entry.getArrivalMinute();
    }
}
