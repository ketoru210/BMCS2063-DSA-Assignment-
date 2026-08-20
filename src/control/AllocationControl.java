package control;

import adt.CollectionInterface;
import adt.MaxHeap;
import dao.AllocationDAO;
import entity.Allocation;
import entity.Booking;
import entity.LoyaltyTier;
import entity.Room;
import entity.RoomType;
import entity.SpecialCategory;
import java.util.Iterator;

/**
 * Control for M2 - VIP &amp; Loyalty Tier-Priority Room Allocation.
 * <p>
 * Owns the waiting queue and the simulated clock. The sort key is computed here
 * and stored on the entry before it enters the heap, so the heap itself never
 * reads a clock and never has to be rebuilt as simulated time advances.
 * <p>
 * The clock is simulated rather than real: against a wall clock every seeded
 * request would arrive at minute zero and none of the tier-versus-waiting
 * crossovers would ever be visible.
 * <p>
 * Bookings are read through M4's control and rooms through M3's, never through
 * a DAO: a DAO is private to its own module, and going through the control is
 * what keeps every module pointing at one set of objects.
 *
 * @author YZ
 */
public class AllocationControl {
    // only Confirmed bookings queue for a room; move to utility/ when M4 needs it too
    private static final String STATUS_CONFIRMED = "Confirmed";

    // a room is allocatable only when both statuses agree; M3 owns the second one
    private static final String OCCUPANCY_AVAILABLE = "Available";
    private static final String OCCUPANCY_RESERVED = "Reserved";
    private static final String HOUSEKEEPING_READY = "Ready for Check-In";

    // the heap type stays reachable because bulk build, height and level order
    // are not expressible on an implementation-neutral ADT
    private final MaxHeap<Allocation> heap = new MaxHeap<>();

    // the same object again through the specification, so every operation the
    // team ADT already covers is written against it and nothing has to downcast
    private final CollectionInterface<Allocation> queue = heap;

    private final BookingControl bookingControl;

    // rooms belong to M3; allocation only flips Available -> Reserved
    private final HousekeepingControl housekeepingControl;

    private int nextEntryNo = 1;
    private int clockMinute;
    private int lastDiscarded;

    public AllocationControl(BookingControl bookingControl, HousekeepingControl housekeepingControl) {
        this.bookingControl = bookingControl;
        this.housekeepingControl = housekeepingControl;
        clockMinute = AllocationDAO.getSeedNowMinute();

        CollectionInterface<Allocation> seeded =
                new AllocationDAO().getAllRequests(bookingControl.getAllBookings());

        // every entry is keyed before the heap is built, so the rebuild below
        // sees the real priorities rather than the unset ones the DAO ordered on
        Iterator<Allocation> walker = seeded.getIterator();
        while (walker.hasNext()) {
            Allocation entry = walker.next();
            entry.setInvariantPriority(computeInvariantPriority(entry));
            if (entry.getEntryNo() >= nextEntryNo) {
                nextEntryNo = entry.getEntryNo() + 1;
            }
        }
        heap.addAll(seeded);
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
        // the same test the candidate list is built from, so a booking that would
        // never be offered cannot be pushed in by any other caller either
        if (booking == null || category == null || !isQueueable(booking)) {
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

    /** Lowest priority entry - the one to watch when checking for starvation. */
    public Allocation peekLast() {
        return queue.getLast();
    }

    /**
     * Level order, which for this heap is array order - NOT priority order.
     * <p>
     * Taken straight off the heap's own array: walking an iterator to rebuild
     * an array the representation already holds is work for nothing.
     */
    public Allocation[] getLevelOrder() {
        Object[] slots = heap.toLevelOrderArray();
        Allocation[] entries = new Allocation[slots.length];
        for (int i = 0; i < slots.length; i++) {
            entries[i] = (Allocation) slots[i];
        }
        return entries;
    }

    /** Levels the queue spans when drawn as the tree it is; 0 when empty. */
    public int getQueueHeight() {
        return heap.getHeight();
    }

    /**
     * The queue in the order it will actually be served, highest priority first.
     * <p>
     * Insertion sort rather than a divide-and-conquer sort: level order already
     * puts every parent ahead of its own descendants, so the array arrives
     * partially ordered and the inner loop seldom runs. The cost is proportional
     * to the number of inversions, which on a heap layout is far below n squared.
     */
    public Allocation[] getServeOrder() {
        Allocation[] entries = getLevelOrder();

        for (int i = 1; i < entries.length; i++) {
            // hold the entry aside and shift the bigger ones right, as sift-up does
            Allocation moving = entries[i];
            int j = i - 1;
            while (j >= 0 && entries[j].compareTo(moving) < 0) {
                entries[j + 1] = entries[j];
                j--;
            }
            entries[j + 1] = moving;
        }
        return entries;
    }

    /** Confirmed bookings that have no room yet and are not already waiting. */
    public Booking[] getQueueableBookings() {
        Booking[] all = bookingControl.getAllBookings();

        int count = 0;
        for (Booking booking : all) {
            if (isQueueable(booking)) {
                count++;
            }
        }

        Booking[] queueable = new Booking[count];
        int filled = 0;
        for (Booking booking : all) {
            if (isQueueable(booking)) {
                queueable[filled] = booking;
                filled++;
            }
        }
        return queueable;
    }

    private boolean isQueueable(Booking booking) {
        // goes through the collection: the same booking must not queue twice
        return isServable(booking)
                && findByConfirmationNo(booking.getConfirmationNo()) == null;
    }

    /**
     * Whether the booking behind a queued entry is still one a room may be given
     * to. A request outlives the booking it was made for whenever the desk
     * cancels or otherwise moves that booking on.
     */
    private boolean isServable(Booking booking) {
        return STATUS_CONFIRMED.equals(booking.getStatus()) && !booking.isAllocated();
    }

    // search(probe) needs a probe carrying the whole sort key, and a confirmation
    // number alone cannot build one - so this lookup walks instead
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

    /**
     * Which identifier a search is asking about. Naming it is not a convenience:
     * an entry no. and a confirmation no. can be the same width, so a term alone
     * cannot say which one was meant.
     */
    public enum SearchKey {
        ENTRY_NO("Entry no."),
        CONFIRMATION_NO("Confirmation no."),
        // not "guest": GUEST is a tier, and this is the person's name whatever their tier
        CUSTOMER_NAME("Customer name");

        private final String label;

        SearchKey(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Entries whose chosen identifier answers to the term. That identifier is
     * never the sort key, so no ordering can be exploited and the walk is the
     * honest cost; what the heap does give cheaply is where each hit lands in
     * the serve order.
     */
    public Allocation[] search(SearchKey key, String term) {
        if (key == null || term == null || term.isBlank()) {
            return new Allocation[0];
        }
        String needle = term.trim().toLowerCase();

        Allocation[] hits = new Allocation[queue.size()];
        int found = 0;
        Iterator<Allocation> walker = queue.getIterator();
        while (walker.hasNext()) {
            Allocation entry = walker.next();
            if (answersTo(entry, key, needle)) {
                hits[found++] = entry;
            }
        }

        Allocation[] exact = new Allocation[found];
        for (int i = 0; i < found; i++) {
            exact[i] = hits[i];
        }
        return exact;
    }

    private boolean answersTo(Allocation entry, SearchKey key, String needle) {
        switch (key) {
            case ENTRY_NO:
                String typed = needle.startsWith("#") ? needle.substring(1) : needle;
                return typed.equals(String.valueOf(entry.getEntryNo()));
            case CONFIRMATION_NO:
                return needle.equals(entry.getBooking().getConfirmationNo().toLowerCase());
            case CUSTOMER_NAME:
                // a partial name is what somebody at the desk actually has to go on
                return entry.getBooking().getMember().getName().toLowerCase().contains(needle);
            default:
                return false;
        }
    }

    /** Where the entry sits in the serve order, counted from 1, without sorting. */
    public int getServePosition(Allocation entry) {
        return entry == null ? 0 : heap.rankOf(entry) + 1;
    }

    /** Score to show the user: always positive, and ranks exactly like the stored key. */
    public long livePriority(Allocation entry) {
        return entry.getInvariantPriority() + clockMinute;
    }

    // --- delete ---

    /**
     * Allocates a room to the highest priority entry and removes it from the
     * queue. Returns the entry served, or null if there was nobody left waiting
     * or no room was ready.
     * <p>
     * The queue is read before it is written: with no suitable room the guest
     * keeps their place rather than being dropped, so the caller can tell the
     * two failures apart by asking whether the queue is still empty.
     * <p>
     * Entries whose booking has since been cancelled or housed by another route
     * are discarded on the way. M4 pulls them out at the moment it cancels, so
     * this is the fuse rather than the mechanism, and how many it burnt is
     * readable from {@link #getLastDiscarded()} until the next call.
     */
    public Allocation serveNext() {
        lastDiscarded = 0;

        for (;;) {
            Allocation next = queue.getFirst();
            if (next == null) {
                return null;
            }
            if (!isServable(next.getBooking())) {
                queue.remove();
                lastDiscarded++;
                continue;
            }

            Room room = pickRoom(next.getBooking().getRoomType());
            if (room == null) {
                return null;
            }

            queue.remove();
            next.getBooking().setRoom(room);
            room.setOccupancyStatus(OCCUPANCY_RESERVED);
            return next;
        }
    }

    /** Stale entries dropped by the most recent serveNext; reset by each call. */
    public int getLastDiscarded() {
        return lastDiscarded;
    }

    /**
     * The cheapest ready room that is not a downgrade, preferring an exact
     * match. A guest is never given less than the type they are billed for, but
     * a suite is not spent on a single-room booking while a single is free.
     */
    private Room pickRoom(RoomType requested) {
        Room[] rooms = housekeepingControl.getAllRooms();
        int index = pickIndex(rooms, null, requested);
        return index < 0 ? null : rooms[index];
    }

    /**
     * The choice itself, expressed over any pool so that the dry-run report and
     * a real serve cannot drift apart — a forecast produced by a second copy of
     * this rule would eventually stop predicting what the desk actually does.
     * A null mask means nothing is spoken for yet.
     */
    private int pickIndex(Room[] pool, boolean[] taken, RoomType requested) {
        int best = -1;
        for (int i = 0; i < pool.length; i++) {
            Room room = pool[i];
            if ((taken != null && taken[i]) || !isAllocatable(room)) {
                continue;
            }
            if (room.getRoomType() == requested) {
                return i;
            }
            if (room.getRoomType().getRatePerNight() < requested.getRatePerNight()) {
                continue;
            }
            if (best < 0
                    || room.getRoomType().getRatePerNight() < pool[best].getRoomType().getRatePerNight()) {
                best = i;
            }
        }
        return best;
    }

    private boolean isAllocatable(Room room) {
        return OCCUPANCY_AVAILABLE.equals(room.getOccupancyStatus())
                && HOUSEKEEPING_READY.equals(room.getHousekeepingStatus());
    }

    /** How many rooms M2 could hand out right now, whatever the type. */
    public int getReadyRoomCount() {
        Room[] rooms = housekeepingControl.getAllRooms();
        int ready = 0;
        for (Room room : rooms) {
            if (isAllocatable(room)) {
                ready++;
            }
        }
        return ready;
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
     * Tier weight minus arrival minute - the priority the entry would have at
     * minute zero. The live score adds the same clock reading to everybody, so
     * dropping it changes no ordering and the stored key never goes stale.
     */
    private long computeInvariantPriority(Allocation entry) {
        return entry.getTier().getWeight() - entry.getArrivalMinute();
    }

    // ================= report criteria =================
    //
    // Field and operator are arguments rather than compiled-in choices, so a new
    // question costs a new enum constant instead of a new report method.

    /** A per-entry quantity a report can order or filter on. */
    public enum Field {
        ENTRY_NO("Entry no."),
        ARRIVAL("Arrival minute"),
        WAITED("Minutes waited"),
        TIER_WEIGHT("Tier weight"),
        LIVE_PRIORITY("Live priority"),
        EXPOSURE("Exposure remaining");

        private final String label;

        Field(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum Operator {
        GREATER(">"), GREATER_EQUAL(">="), EQUAL("="), LESSER_EQUAL("<="), LESSER("<");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    public long fieldValue(Allocation entry, Field field, LoyaltyTier challenger) {
        return switch (field) {
            case ENTRY_NO -> entry.getEntryNo();
            case ARRIVAL -> entry.getArrivalMinute();
            case WAITED -> getWaited(entry);
            case TIER_WEIGHT -> entry.getTier().getWeight();
            case LIVE_PRIORITY -> livePriority(entry);
            case EXPOSURE -> getExposure(entry, challenger);
            default -> 0;
        };
    }

    /** Keeps the entries whose chosen field stands in the chosen relation to value. */
    public Allocation[] filterBy(Allocation[] source, Field field, Operator op, long value,
                                 LoyaltyTier challenger) {
        int kept = 0;
        for (Allocation allocation : source) {
            if (matches(allocation, field, op, value, challenger)) {
                kept++;
            }
        }

        Allocation[] result = new Allocation[kept];
        int filled = 0;
        for (Allocation allocation : source) {
            if (matches(allocation, field, op, value, challenger)) {
                result[filled] = allocation;
                filled++;
            }
        }
        return result;
    }

    private boolean matches(Allocation entry, Field field, Operator op, long value,
                            LoyaltyTier challenger) {
        long actual = fieldValue(entry, field, challenger);
        return switch (op) {
            case GREATER -> actual > value;
            case GREATER_EQUAL -> actual >= value;
            case EQUAL -> actual == value;
            case LESSER_EQUAL -> actual <= value;
            case LESSER -> actual < value;
            default -> true;
        };
    }

    /**
     * Sorts a copy on the chosen field, leaving the caller's array alone.
     * <p>
     * Merge sort rather than the insertion sort used for serve order: against an
     * arbitrary report field the heap's level order carries no head start, so the
     * near-sorted assumption that makes insertion sort cheap no longer holds.
     * Merging is also stable, so entries that tie on the field stay in serve
     * order and the same question always produces the same table.
     */
    public Allocation[] sortBy(Allocation[] source, Field field, boolean descending,
                               LoyaltyTier challenger) {
        Allocation[] sorted = new Allocation[source.length];
        for (int i = 0; i < source.length; i++) {
            sorted[i] = source[i];
        }
        mergeSort(sorted, new Allocation[source.length], 0, sorted.length - 1,
                field, descending, challenger);
        return sorted;
    }

    private void mergeSort(Allocation[] entries, Allocation[] buffer, int low, int high,
                           Field field, boolean descending, LoyaltyTier challenger) {
        if (low >= high) {
            return;
        }
        // written this way rather than (low + high) / 2, which overflows once the
        // two ends are large enough to sum past Integer.MAX_VALUE
        int mid = low + (high - low) / 2;

        mergeSort(entries, buffer, low, mid, field, descending, challenger);
        mergeSort(entries, buffer, mid + 1, high, field, descending, challenger);
        merge(entries, buffer, low, mid, high, field, descending, challenger);
    }

    private void merge(Allocation[] entries, Allocation[] buffer, int low, int mid, int high,
                       Field field, boolean descending, LoyaltyTier challenger) {
        for (int i = low; i <= high; i++) {
            buffer[i] = entries[i];
        }

        int left = low;
        int right = mid + 1;
        for (int i = low; i <= high; i++) {
            if (left > mid) {
                entries[i] = buffer[right];
                right++;
            } else if (right > high) {
                entries[i] = buffer[left];
                left++;
            } else if (leftFirst(buffer[left], buffer[right], field, descending, challenger)) {
                entries[i] = buffer[left];
                left++;
            } else {
                entries[i] = buffer[right];
                right++;
            }
        }
    }

    /** Ties answer true, which is exactly what keeps the merge stable. */
    private boolean leftFirst(Allocation left, Allocation right, Field field, boolean descending,
                              LoyaltyTier challenger) {
        int order = Long.compare(fieldValue(left, field, challenger),
                fieldValue(right, field, challenger));
        return descending ? order >= 0 : order <= 0;
    }

    // ================= report 1: starvation guarantee =================

    public int getWaited(Allocation entry) {
        return clockMinute - entry.getArrivalMinute();
    }

    /**
     * The simulated minute from which no newly arriving guest of that tier can
     * still overtake this entry.
     * <p>
     * A newcomer arriving at minute t scores {@code challengerWeight - t} and this
     * entry scores {@code tierWeight - arrivalMinute}; the live clock is added to
     * both, so it cancels. Solving for the t at which the newcomer stops winning
     * gives the minute below — which is why waiting buys protection from future
     * arrivals rather than movement against the guests already in the queue.
     */
    public int getImmuneAt(Allocation entry, LoyaltyTier challenger) {
        int crossing = entry.getArrivalMinute() + challenger.getWeight() - entry.getTier().getWeight();
        // an entry outranking the challenger from the start crosses before minute
        // zero, and a negative minute has no meaning on a clock that starts there
        return Math.max(crossing, 0);
    }

    /**
     * Minutes this entry can still be overtaken for; zero once it is safe. A
     * special category is compared as a layer above the tier
     * score, so an
     * ordinary arrival never reaches it however high its tier.
     */
    public int getExposure(Allocation entry, LoyaltyTier challenger) {
        if (isCategoryProtected(entry)) {
            return 0;
        }
        int remaining = getImmuneAt(entry, challenger) - clockMinute;
        return Math.max(remaining, 0);
    }

    public boolean isCategoryProtected(Allocation entry) {
        return entry.getCategory() != SpecialCategory.NONE;
    }

    public int countProtected(LoyaltyTier challenger) {
        Iterator<Allocation> walker = queue.getIterator();
        int safe = 0;
        while (walker.hasNext()) {
            if (getExposure(walker.next(), challenger) == 0) {
                safe++;
            }
        }
        return safe;
    }

    /** The entry exposed for longest — the queue's weakest place. */
    public Allocation getMostExposed(LoyaltyTier challenger) {
        Iterator<Allocation> walker = queue.getIterator();
        Allocation worst = null;
        while (walker.hasNext()) {
            Allocation entry = walker.next();
            if (worst == null
                    || getExposure(entry, challenger) > getExposure(worst, challenger)) {
                worst = entry;
            }
        }
        return worst;
    }

    /** The minute after which the queue as it stands can no longer be jumped. */
    public int getSealedAtMinute(LoyaltyTier challenger) {
        Allocation worst = getMostExposed(challenger);
        return worst == null ? clockMinute : clockMinute + getExposure(worst, challenger);
    }

    /** Indexed by LoyaltyTier.ordinal(), so the caller pairs it with values(). */
    public int[] getCountByTier() {
        int[] count = new int[LoyaltyTier.values().length];
        Iterator<Allocation> walker = queue.getIterator();
        while (walker.hasNext()) {
            count[walker.next().getTier().ordinal()]++;
        }
        return count;
    }

    /** Mean exposure per tier; a tier with nobody waiting reads zero. */
    public double[] getMeanExposureByTier(LoyaltyTier challenger) {
        int tiers = LoyaltyTier.values().length;
        double[] total = new double[tiers];
        int[] count = getCountByTier();

        Iterator<Allocation> walker = queue.getIterator();
        while (walker.hasNext()) {
            Allocation entry = walker.next();
            total[entry.getTier().ordinal()] += getExposure(entry, challenger);
        }

        for (int i = 0; i < tiers; i++) {
            if (count[i] > 0) {
                total[i] /= count[i];
            }
        }
        return total;
    }

    // ================= report 2: fulfilment dry-run =================

    /** Waiting entries per room type, indexed by RoomType.ordinal(). */
    public int[] getWaitingByType() {
        int[] waiting = new int[RoomType.values().length];
        Iterator<Allocation> walker = queue.getIterator();
        while (walker.hasNext()) {
            waiting[walker.next().getBooking().getRoomType().ordinal()]++;
        }
        return waiting;
    }

    /** Allocatable rooms per room type, indexed by RoomType.ordinal(). */
    public int[] getReadyByType() {
        int[] ready = new int[RoomType.values().length];
        Room[] rooms = housekeepingControl.getAllRooms();
        for (Room room : rooms) {
            if (isAllocatable(room)) {
                ready[room.getRoomType().ordinal()]++;
            }
        }
        return ready;
    }

    /** What allocating the queue on paper would produce. Read-only by construction. */
    public static final class Forecast {
        private int queued;
        private int served;
        private Allocation blocker;
        private int upgrades;
        private double upgradeCost;
        private int[] waitingByType;
        private int[] readyByType;
        private Room releasable;
        private int unblockGain;

        public int getQueued() { return queued; }

        public int getServed() { return served; }

        /** The first entry no room fits; null when the whole look-ahead clears. */
        public Allocation getBlocker() { return blocker; }

        /** Entries stuck behind the blocker purely because it is at the front. */
        public int getBlocked() { return blocker == null ? 0 : queued - served - 1; }

        public int getUpgrades() { return upgrades; }

        /** Rate difference times nights — what the upgrades give away. */
        public double getUpgradeCost() { return upgradeCost; }

        public int[] getWaitingByType() { return waitingByType; }

        public int[] getReadyByType() { return readyByType; }

        /** A vacant room housekeeping could clean to break the stall; may be null. */
        public Room getReleasable() { return releasable; }

        /** Extra guests served if one more room of the blocked type were ready. */
        public int getUnblockGain() { return unblockGain; }
    }

    /**
     * Allocates the queue on paper, changing nothing: the rooms are spoken for in
     * a local mask, so the desk can ask as often as it likes and the real
     * registry never moves. A look-ahead of zero or less runs until it stalls.
     */
    public Forecast forecast(int lookAhead) {
        Allocation[] order = getServeOrder();
        Room[] pool = housekeepingControl.getAllRooms();
        int limit = (lookAhead > 0 && lookAhead < order.length) ? lookAhead : order.length;

        Forecast forecast = run(order, pool, limit);
        forecast.queued = order.length;
        forecast.waitingByType = getWaitingByType();
        forecast.readyByType = getReadyByType();

        if (forecast.blocker != null) {
            RoomType wanted = forecast.blocker.getBooking().getRoomType();
            forecast.releasable = findReleasable(pool, wanted);
            forecast.unblockGain = run(order, withOneMore(pool, wanted), limit).served
                    - forecast.served;
        }
        return forecast;
    }

    private Forecast run(Allocation[] order, Room[] pool, int limit) {
        Forecast forecast = new Forecast();
        boolean[] taken = new boolean[pool.length];

        for (int i = 0; i < limit; i++) {
            Booking booking = order[i].getBooking();
            int index = pickIndex(pool, taken, booking.getRoomType());
            if (index < 0) {
                forecast.blocker = order[i];
                return forecast;
            }

            taken[index] = true;
            forecast.served++;

            RoomType given = pool[index].getRoomType();
            if (given != booking.getRoomType()) {
                forecast.upgrades++;
                forecast.upgradeCost +=
                        (given.getRatePerNight() - booking.getRoomType().getRatePerNight())
                                * booking.getNights();
            }
        }
        return forecast;
    }

    /** A real room that is already vacant and only needs cleaning; null if none. */
    private Room findReleasable(Room[] pool, RoomType type) {
        for (Room room : pool) {
            if (room.getRoomType() == type
                    && OCCUPANCY_AVAILABLE.equals(room.getOccupancyStatus())
                    && !HOUSEKEEPING_READY.equals(room.getHousekeepingStatus())) {
                return room;
            }
        }
        return null;
    }

    /** The pool plus one hypothetical ready room; it exists only inside the dry run. */
    private Room[] withOneMore(Room[] pool, RoomType type) {
        Room[] extended = new Room[pool.length + 1];
        for (int i = 0; i < pool.length; i++) {
            extended[i] = pool[i];
        }
        extended[pool.length] =
                new Room("(hypothetical)", type, OCCUPANCY_AVAILABLE, HOUSEKEEPING_READY);
        return extended;
    }
}
