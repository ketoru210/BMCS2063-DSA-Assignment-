package entity;

import java.io.Serializable;

/**
 * One waiting entry in the tier-priority allocation queue.
 * <p>
 * Ordering is layered: special category first, then the aging-adjusted tier
 * score, then arrival order. The score is computed once by the control and
 * stored here — nothing in this class or in the heap ever reads a clock, so the
 * heap stays valid as time passes and never has to be rebuilt.
 *
 * @author YZ
 */
public class Allocation implements Serializable, Comparable<Allocation> {
    private final Booking booking;
    private final SpecialCategory special;
    private final int arrivalMinute;
    private final int seq;

    private long invariantPriority;

    public Allocation(Booking booking, SpecialCategory special, int arrivalMinute, int seq) {
        this.booking = booking;
        this.special = special;
        this.arrivalMinute = arrivalMinute;
        this.seq = seq;
    }

    public Booking getBooking() {
        return booking;
    }

    /**
     * The member's tier, read live — the stored key keeps whatever tier was in
     * force when it was computed, so a tier change needs a deliberate reprioritise.
     */
    public LoyaltyTier getTier() {
        return booking.getMember().getCurrentTier();
    }

    public SpecialCategory getSpecial() {
        return special;
    }

    public int getArrivalMinute() {
        return arrivalMinute;
    }

    public int getSeq() {
        return seq;
    }

    public long getInvariantPriority() {
        return invariantPriority;
    }

    // set by the control before add(), and again only on a deliberate reprioritise
    public void setInvariantPriority(long invariantPriority) {
        this.invariantPriority = invariantPriority;
    }

    @Override
    public int compareTo(Allocation other) {
        int bySpecial = Integer.compare(special.getWeight(), other.special.getWeight());
        if (bySpecial != 0) {
            return bySpecial;
        }

        // inside a special band an ambulance does not check loyalty cards
        if (special == SpecialCategory.NONE) {
            int byPriority = Long.compare(invariantPriority, other.invariantPriority);
            if (byPriority != 0) {
                return byPriority;
            }
        }

        // the smaller seq arrived first, but a max-heap serves the greater element
        return Integer.compare(other.seq, seq);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return seq == ((Allocation) obj).seq;
    }

    @Override
    public String toString() {
        return String.format("#%-3d | %-18s | %-8s | %-12s | arrived %3d min | S=%d",
                seq,
                booking.getMember().getName(),
                getTier(),
                special,
                arrivalMinute,
                invariantPriority);
    }
}
