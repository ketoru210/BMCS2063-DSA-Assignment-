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
 * @author QW
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

    // ---- desk operations ----

    public Booking createWalkIn(String guestName, RoomType roomType, int nights){
        if(guestName == null || guestName.isEmpty() || roomType == null || nights < 1){
            return null;
        }
        // GUEST, not the constructor's default of SILVER: a walk-in has not joined
        // the programme, and M2 would otherwise hand them 30 minutes of priority
        Member guest = new Member(guestName.toLowerCase().replace(" ",""), "walkin", guestName,
                LoyaltyTier.GUEST);
        LocalDate checkIn = LocalDate.now();
        
        Booking booking;
        do{
            booking = new Booking(nextConfirmationNo(), guest, roomType, checkIn, checkIn.plusDays(nights), STATUS_CONFIRMED);
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
    // public Booking findByConfirmationNo(String confirmationNo) {
    //     if (confirmationNo == null) {
    //         return null;
    //     }
    //     for (int i = 0; i < bookings.length; i++) {
    //         if (bookings[i].getConfirmationNo().equals(confirmationNo)) {
    //             return bookings[i];
    //         }
    //     }
    //     return null;
    // }
    //}
