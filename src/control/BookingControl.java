package control;

import adt.BinarySearchTree;
import adt.CollectionInterface;
import dao.BookingDAO;
import entity.Allocation;
import entity.Booking;
import entity.LoyaltyTier;
import entity.Member;
import entity.Room;
import entity.RoomType;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Random;

/**
 * Control for M4 — Front-Desk Service.
 * <p>
 * A DAO is read by its owning module's control and by nobody else — every other
 * module goes through this class. Combined with there being exactly one instance
 * of it, created in {@code Main}, that is what keeps M2 and M4 looking at one
 * set of Booking objects rather than two.
 * <p>
 * The desk drives the whole life of a booking — Confirmed, given a room by M2,
 * Checked-in, Checked-out, or Cancelled — so it is also where a booking that
 * changes state has to reach the other modules holding it: M2's queue when it is
 * cancelled, M3's task log when the guest leaves.
 *
 * @author Fong Qin Wen
 */
public class BookingControl {

    private static final int FIRST_CONFIRMATION_NO = 10000000;
    private static final int CONFIRMATION_NO_RANGE = 90000000;

    private static final String STATUS_CONFIRMED = "Confirmed";
    private static final String STATUS_CHECKED_IN = "Checked-in";
    private static final String STATUS_CHECKED_OUT = "Checked-out";
    private static final String STATUS_CANCELLED = "Cancelled";

    private static final String OCCUPANCY_AVAILABLE = "Available";
    private static final String OCCUPANCY_OCCUPIED = "Occupied";

    // the tree type stays reachable because height, level order and predecessor
    // are not expressible on an implementation-neutral ADT
    private final BinarySearchTree<Booking> registry = new BinarySearchTree<>();

    // ADT Declaration
    // the same object through the specification, so no call site has to downcast
    private final CollectionInterface<Booking> bookings = registry;

    private final Random random = new Random();

    // a checkout hands the room straight back to M3, dirty
    private final HousekeepingControl housekeepingControl;

    // set by Main once both exist: M2 reads bookings from here and a cancellation
    // has to reach M2's queue, so one of the two directions must be wired late
    private AllocationControl allocationControl;

    // members live in M5; the desk books on their behalf and never touches
    // MemberDAO itself, so the whole of that traffic goes through this one field
    private LoyaltyControl loyaltyControl;

    private int rerolls;

    // private final Booking[] bookings;

    public BookingControl(HousekeepingControl housekeepingControl) {
        this.housekeepingControl = housekeepingControl;

        Iterator<Booking> seeded = new BookingDAO().getAllBookings().getIterator();
        while (seeded.hasNext()) {
            bookings.add(seeded.next());
        }
    }

    public void attachAllocation(AllocationControl allocationControl) {
        this.allocationControl = allocationControl;
    }

    public void attachLoyalty(LoyaltyControl loyaltyControl) {
        this.loyaltyControl = loyaltyControl;
    }

    
    // ---- lookup ----
 
    /**
     * Ascending by confirmation number, because that is the tree's traversal order.
     * Returned as an array so M2 keeps compiling against the same signature.
     */

    public Booking[] getAllBookings() {
        Booking[] all = new Booking[bookings.size()];
        Iterator<Booking> walker = bookings.getIterator();
        int i = 0;
        while(walker.hasNext()){
            all[i++] = walker.next();
        }
        return all;
    }

    public Booking findByConfirmationNo(String confirmationNo){
        return bookings.search(probe(confirmationNo));
    }

    /** The stored booking just below a miss, so the desk can offer a near match. */
    public Booking nearestBelow(String confirmationNo){
        return registry.predecessorOf(probe(confirmationNo));
    }

    public Booking nearestAbove(String confirmationNo){
        return registry.successorOf(probe(confirmationNo));
    }

    /**
     * compareTo reads only the confirmation number, so the remaining fields are
     * never touched — filling them would only invite them to be trusted.
     */
    private Booking probe(String confirmationNo){
        if(confirmationNo == null || confirmationNo.isEmpty()){
            return null;
        }
        return new Booking(confirmationNo, null, null, null, null, null);
    }

    // ---- member lookup ----

    public Member[] getMembers() {
        return loyaltyControl == null ? new Member[0] : loyaltyControl.getAllMembers();
    }

    /**
     * Whoever the desk can identify from what the guest said. An exact member ID
     * or username wins on its own; anything else is treated as a piece of a name,
     * because that is all a caller on the phone usually gives.
     */
    public Member[] searchMembers(String query) {
        if (query == null || query.isEmpty()) {
            return new Member[0];
        }
        Member[] all = getMembers();
        String padded = asMemberID(query);

        for (Member member : all) {
            if (member.getMemberID().equalsIgnoreCase(query)
                    || member.getMemberID().equals(padded)
                    || member.getUsername().equalsIgnoreCase(query)) {
                return new Member[]{member};
            }
        }

        String needle = query.toLowerCase();
        Member[] buffer = new Member[all.length];
        int matched = 0;
        for (Member member : all) {
            if (member.getName().toLowerCase().contains(needle)) {
                buffer[matched++] = member;
            }
        }
        Member[] found = new Member[matched];
        for (int i = 0; i < matched; i++) {
            found[i] = buffer[i];
        }
        return found;
    }

    /**
     * A bare number read as the member ID it is short for, so the desk can type
     * the 1 it sees rather than M00001. Null when the query is not a bare number,
     * which leaves it to be matched as a name instead.
     */
    private String asMemberID(String query) {
        if (query.length() > 5) {
            return null;
        }
        for (int i = 0; i < query.length(); i++) {
            if (!Character.isDigit(query.charAt(i))) {
                return null;
            }
        }
        return String.format("M%05d", Integer.parseInt(query));
    }

    /** Everything the registry holds for one member, in the tree's in-order walk. */
    public Booking[] bookingsOf(Member member) {
        if (member == null) {
            return new Booking[0];
        }
        Booking[] buffer = new Booking[bookings.size()];
        int matched = 0;
        Iterator<Booking> walker = bookings.getIterator();
        while (walker.hasNext()) {
            Booking booking = walker.next();
            if (member.equals(booking.getMember())) {
                buffer[matched++] = booking;
            }
        }
        Booking[] found = new Booking[matched];
        for (int i = 0; i < matched; i++) {
            found[i] = buffer[i];
        }
        return found;
    }

    /**
     * The live stay this member already holds that overlaps the requested dates,
     * or null when the dates are free. Cancelled and checked-out stays are closed
     * business and cannot clash; a stay that ends on the day another begins does
     * not overlap, because the room is given up that morning.
     */
    public Booking findClashingStay(Member member, LocalDate checkIn, LocalDate checkOut) {
        if (member == null || checkIn == null || checkOut == null) {
            return null;
        }
        Iterator<Booking> walker = bookings.getIterator();
        while (walker.hasNext()) {
            Booking booking = walker.next();
            if (!member.equals(booking.getMember())
                    || STATUS_CANCELLED.equals(booking.getStatus())
                    || STATUS_CHECKED_OUT.equals(booking.getStatus())) {
                continue;
            }
            if (checkIn.isBefore(booking.getCheckOut()) && booking.getCheckIn().isBefore(checkOut)) {
                return booking;
            }
        }
        return null;
    }

    // ---- room pool, as M2 sees it ----

    /**
     * Rooms that could be handed over right now, per room type. Read straight off
     * M2 rather than off M3: allocatable means Available and cleaned, and which
     * pair of statuses that is belongs to the module that hands rooms out.
     * <p>
     * This is a snapshot of this minute, not of the stay being booked - a room is
     * held from the moment it is allocated, and nothing in the system records what
     * a given room will be doing on a date in October.
     */
    public int[] readyByType(){
        return allocationControl == null
                ? new int[RoomType.values().length]
                : allocationControl.getReadyByType();
    }

    /** Bookings already queued for each room type, which is the competition. */
    public int[] waitingByType(){
        return allocationControl == null
                ? new int[RoomType.values().length]
                : allocationControl.getWaitingByType();
    }

    // ---- desk operations ----

    public Booking createWalkIn(String guestName, RoomType roomType, int nights){
        if(guestName == null || guestName.isEmpty() || nights < 1){
            return null;
        }
        // GUEST, not the constructor's default of SILVER: a walk-in has not joined
        // the programme, and M2 would otherwise hand them 30 minutes of priority
        Member guest = new Member(guestName.toLowerCase().replace(" ",""), "walkin", guestName,
                LoyaltyTier.GUEST);
        LocalDate checkIn = LocalDate.now();
        return createBooking(guest, roomType, checkIn, checkIn.plusDays(nights));
    }

    /**
     * Books an existing member, so unlike a walk-in the stay can start on a future
     * date and the tier the member already carries is what M2 will queue them on.
     * <p>
     * The confirmation number is rerolled until the tree accepts it: add() refuses
     * a duplicate key, which is the only check that the number is actually free.
     */
    public Booking createBooking(Member member, RoomType roomType, LocalDate checkIn, LocalDate checkOut){
        if(member == null || roomType == null || checkIn == null || checkOut == null
                || !checkOut.isAfter(checkIn)){
            return null;
        }

        Booking booking;
        do{
            booking = new Booking(nextConfirmationNo(), member, roomType, checkIn, checkOut, STATUS_CONFIRMED);
            if(!bookings.add(booking)){
                rerolls++;
                booking = null;
            }
        }while(booking == null);
        return booking;
    }

    private String nextConfirmationNo(){
        return String.valueOf(FIRST_CONFIRMATION_NO + random.nextInt(CONFIRMATION_NO_RANGE));
    }

    /**
     * A guest cannot be put into a room they have not been given, so the room is
     * a precondition rather than an optional extra. That also keeps a booking
     * still waiting in M2's queue out of the check-in path, because a queued
     * booking has no room by definition.
     */
    public boolean checkIn(String confirmationNo){
        Booking booking = findByConfirmationNo(confirmationNo);
        if(booking == null || !STATUS_CONFIRMED.equals(booking.getStatus())
                || !booking.isAllocated()){
            return false;
        }
        booking.setStatus(STATUS_CHECKED_IN);
        booking.getRoom().setOccupancyStatus(OCCUPANCY_OCCUPIED);
        return true;
    }

    /**
     * Ends the stay. The room goes back to the pool dirty, which is the only
     * route by which M3 is given work and M2 gets rooms back — without it the
     * registry only ever consumes rooms.
     */
    public boolean checkOut(String confirmationNo){
        Booking booking = findByConfirmationNo(confirmationNo);
        if(booking == null || !STATUS_CHECKED_IN.equals(booking.getStatus())
                || !booking.isAllocated()){
            return false;
        }

        Room room = booking.getRoom();
        booking.setStatus(STATUS_CHECKED_OUT);
        booking.setRoom(null);
        room.setOccupancyStatus(OCCUPANCY_AVAILABLE);
        housekeepingControl.markDirty(room);
        return true;
    }

    /**
     * Marks the booking cancelled and lets go of whatever it was holding. The
     * node stays in the tree: a registry that forgets a cancellation cannot be
     * audited, and the status field already carries the distinction. Use
     * {@link #purgeCancelled()} to actually drop them.
     */
    public Booking cancel(String confirmationNo){
        Booking booking = findByConfirmationNo(confirmationNo);
        if(!isCancellable(booking)){
            return null;
        }

        // out of M2's queue first, or the request outlives the booking it is for
        if(allocationControl != null){
            Allocation queued = allocationControl.findByConfirmationNo(confirmationNo);
            if(queued != null){
                allocationControl.cancel(queued);
            }
        }

        if(booking.isAllocated()){
            Room room = booking.getRoom();
            boolean sleptIn = STATUS_CHECKED_IN.equals(booking.getStatus());

            booking.setRoom(null);
            room.setOccupancyStatus(OCCUPANCY_AVAILABLE);
            // a room the guest was already in cannot go straight back on sale
            if(sleptIn){
                housekeepingControl.markDirty(room);
            }
        }
        booking.setStatus(STATUS_CANCELLED);
        return booking;
    }

    /** A stay already finished or already called off cannot be called off again. */
    public boolean isCancellable(Booking booking){
        return booking != null
                && !STATUS_CANCELLED.equals(booking.getStatus())
                && !STATUS_CHECKED_OUT.equals(booking.getStatus());
    }

    /**
     * Drops the cancelled bookings from the registry for good. Cancelling only
     * marks, so this is the one operation that shrinks the tree — and therefore
     * the only place the desk can watch the height respond.
     */
    public int purgeCancelled(){
        Booking[] doomed = new Booking[bookings.size()];
        int found = 0;
        Iterator<Booking> walker = bookings.getIterator();
        while(walker.hasNext()){
            Booking booking = walker.next();
            if(STATUS_CANCELLED.equals(booking.getStatus())){
                doomed[found++] = booking;
            }
        }

        // collected before removing anything: an iterator is not promised to
        // survive a change to the collection it came from
        int removed = 0;
        for(int i = 0; i < found; i++){
            if(bookings.remove(doomed[i])){
                removed++;
            }
        }
        return removed;
    }

        // ---- report 1: tree health ----
        
        public int size(){
            return bookings.size();
        }

        public int getTreeHeight(){
            return registry.getHeight();
        }

        /** Height a perfectly balanced tree of the same size would have. */
        public int getOptimalHeight(){
            int optimal = 0;
            int capacity = 0;
            while(capacity < bookings.size()){
                optimal ++;
                capacity += 1 << (optimal - 1);
            }
            return optimal;
        }

        /** 0% is perfectly balanced, 100% is a linked list. */
        public double getDegeneracyPercent(){
            int optimal = getOptimalHeight();
            int worst = bookings.size();
            if(worst <= optimal){
                return 0.0;
            }
            return(getTreeHeight() - optimal) * 100.0 / (worst - optimal);
        }

        public int getRerolls() {
            return rerolls;
        }
    
        public Booking getLowestConfirmationNo() {
            return bookings.getFirst();
        }
    
        public Booking getHighestConfirmationNo() {
            return bookings.getLast();
        }
    
        // ---- report 2: confirmation range audit ----
        public Booking[] findInRange(String low, String high){
            Booking lowProbe = probe(low);
            Booking highProbe = probe(high);
            if(lowProbe == null || highProbe == null || low.compareTo(high) > 0){
                return new Booking[0];
            }
            Booking[] buffer = new Booking[bookings.size()];
            Iterator<Booking> walker = registry.getRangeIterator(lowProbe, highProbe);
            int matched = 0;
            while(walker.hasNext()){
                buffer[matched++] = walker.next();
            }
            Booking[] found = new Booking[matched];
            for(int i = 0; i < matched; i++){
                found[i] = buffer[i];
            }
            return found;
        }

        /** Which column an audit is ordered on. */
        public enum SortKey {
            CONFIRMATION_NO("Confirmation no."),
            CUSTOMER_NAME("Customer name"),
            NIGHTS("Nights"),
            CHARGE("Charge"),
            STATUS("Status");

            private final String label;

            SortKey(String label){
                this.label = label;
            }

            @Override
            public String toString(){
                return label;
            }
        }

        /**
         * Sorts a copy of the audited batch, leaving the caller's array alone.
         * <p>
         * Insertion sort rather than something with a better worst case: the batch
         * comes straight off the tree's in-order walk, so it arrives already sorted
         * on confirmation no. and near-sorted on anything that tracks it. Insertion
         * sort walks an ordered run at one comparison per entry and only pays for
         * the entries actually out of place, where a divide-and-conquer sort would
         * split and merge the whole range regardless.
         * <p>
         * Shifting rather than swapping: the entry being placed is held aside and
         * the bigger ones slide up, so a move of k places costs k assignments and
         * not 3k.
         */
        public Booking[] sortBy(Booking[] source, SortKey key, boolean descending){
            Booking[] sorted = new Booking[source.length];
            for(int i = 0; i < source.length; i++){
                sorted[i] = source[i];
            }

            for(int i = 1; i < sorted.length; i++){
                Booking moving = sorted[i];
                int j = i - 1;
                // strict, so equal entries never shift: that is what keeps it stable
                while(j >= 0 && outOfOrder(sorted[j], moving, key, descending)){
                    sorted[j + 1] = sorted[j];
                    j--;
                }
                sorted[j + 1] = moving;
            }
            return sorted;
        }

        /** True when settled sits on the wrong side of moving and has to slide up. */
        private boolean outOfOrder(Booking settled, Booking moving, SortKey key, boolean descending){
            int order = compare(settled, moving, key);
            return descending ? order < 0 : order > 0;
        }

        private int compare(Booking left, Booking right, SortKey key){
            switch(key){
                case CUSTOMER_NAME:
                    return left.getMember().getName().compareToIgnoreCase(right.getMember().getName());
                case NIGHTS:
                    return Long.compare(left.getNights(), right.getNights());
                case CHARGE:
                    return Double.compare(chargeOf(left), chargeOf(right));
                case STATUS:
                    return left.getStatus().compareToIgnoreCase(right.getStatus());
                default:
                    return left.getConfirmationNo().compareTo(right.getConfirmationNo());
            }
        }

        /** A cancelled booking is charged nothing, so it sorts as nothing. */
        private double chargeOf(Booking booking){
            return earnsRevenue(booking) ? revenueOf(booking) : 0.0;
        }

        public double revenueOf(Booking booking){
            return booking.getRoomType().getRatePerNight() * booking.getNights();
        }

        /**
         * A cancelled booking is still listed by the audit but earns nothing, so
         * it is left out of every total below. Pending is counted: the room is
         * being held and the money is expected, which is what the audit is for.
         */
        public boolean earnsRevenue(Booking booking){
            return booking != null && !STATUS_CANCELLED.equals(booking.getStatus());
        }

        public int countEarning(Booking[] batch){
            int earning = 0;
            for (Booking booking : batch) {
                if(earnsRevenue(booking)){
                    earning++;
                }
            }
            return earning;
        }

        public double totalRevenueOf(Booking[] batch){
            double total = 0.0;
            for (Booking booking : batch) {
                if(earnsRevenue(booking)){
                    total += revenueOf(booking);
                }
            }
            return total;
        }

        public double averageNightOf(Booking[] batch){
            int earning = countEarning(batch);
            if(earning == 0){
                return 0.0;
            }
            long nights = 0;
            for (Booking booking : batch) {
                if(earnsRevenue(booking)){
                    nights += booking.getNights();
                }
            }
            return (double) nights / earning;
        }

        /** Indexed by Room Type.ordinal(), so the caller pairs it with RoomType.values(). */
        public double[] revenueByRoomType(Booking[] batch){
            double[] revenue = new double[RoomType.values().length];
            for (Booking booking : batch) {
                if(earnsRevenue(booking)){
                    revenue[booking.getRoomType().ordinal()] += revenueOf(booking);
                }
            }
            return revenue;
        }

        /** Interface-typed on purpose: any team collection can be counted this way. */
        public static int countIn(CollectionInterface<?> collection){
            return collection.size();
        }

        /** Slots in level order; a null slot means no node sits at that position. */
        public Booking[] getTreeShape(){
            Object[] slots = registry.toLevelOrderArray();
            Booking[] shape = new Booking[slots.length];
            for(int i = 0; i < slots.length; i++){
                shape[i] = (Booking) slots[i];
            }
            return shape;
        }
    }