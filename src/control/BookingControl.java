package control;

import adt.BinarySearchTree;
import adt.CollectionInterface;
import dao.BookingDAO;
import entity.Booking;
import entity.Member;
import entity.RoomType;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Random;

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
 * @author QW
 */
public class BookingControl {

    private static final int FIRST_CONFIRMATION_NO = 10000000;
    private static final int CONFIRMATION_NO_RANGE = 90000000;

    private final BinarySearchTree<Booking> bookings = new BinarySearchTree<>();
    private final Random random = new Random();

    private int rerolls;

    // private final Booking[] bookings;

    public BookingControl() {
        Booking[] seeded = new BookingDAO().getAllBookings();
        for (int i = 0; i < seeded.length; i++){
            bookings.add(seeded[i]);
        }
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
        return bookings.predecessorOf(probe(confirmationNo));
    }

    public Booking nearestAbove(String confirmationNo){
        return bookings.successorOf(probe(confirmationNo));
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
        Member guest = new Member(guestName.toLowerCase().replace(" ",""), "walkin", guestName);
        LocalDate checkIn = LocalDate.now();
        
        Booking booking;
        do{
            booking = new Booking(nextConfirmationNo(), guest, roomType, checkIn, checkIn.plusDays(nights), "Confirmed");
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

    public boolean checkIn(String confirmationNo){
        Booking booking = findByConfirmationNo(confirmationNo);
        if(booking == null || !"Confirmed".equals(booking.getStatus())){
            return false;
        }
        booking.setStatus("Checked-in");
        if(booking.isAllocated()){
            booking.getRoom().setOccupancyStatus("Occupied");
        }
        return true;
    }

    public Booking cancel(String confirmationNo){
        Booking booking = findByConfirmationNo(confirmationNo);
        if(booking == null){
            return null;
        }
        if(booking.isAllocated()){
            booking.getRoom().setOccupancyStatus("Available");
        }
        return bookings.remove(booking) ? booking : null;
    }

        // ---- report 1: tree health ----
        
        public int size(){
            return bookings.size();
        }

        public int getTreeHeight(){
            return bookings.getHeight();
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
            Iterator<Booking> walker = bookings.getRangeIterator(lowProbe, highProbe);
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

        public double totalRevenueOf(Booking[] batch){
            double total = 0.0;
            for(int i = 0; i < batch.length; i++){
                total += revenueOf(batch[i]);
            }
            return total;
        }

        public double averageNightOf(Booking[] batch){
            if(batch.length == 0){
                return 0.0;
            }
            long nights = 0;
            for(int i = 0; i < batch.length; i++){
                nights += batch[i].getNights();
            }
            return (double) nights / batch.length;
        }

        /** Indexed by Room Type.ordinal(), so the caller pairs it with RoomType.values(). */
        public double[] revenueByRoomType(Booking[] batch){
            double[] revenue = new double[RoomType.values().length];
            for(int i = 0; i < batch.length; i++){
                revenue[batch[i].getRoomType().ordinal()] += revenueOf(batch[i]);
            }
            return revenue;
        }

        /** Interface-typed on purpose: any team collection can be counted this way. */
        public static int countIn(CollectionInterface<?> collection){
            return collection.size();
        }

        /** Slots in level order; a null slot means no node sits at that position. */
        public Booking[] getTreeShape(){
            Object[] slots = bookings.toLevelOrderArray();
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
