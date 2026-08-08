package entity;

/**
 * PLACEHOLDER — field contract per plan.md, replace with the owner's version.
 * <p>
 * Rates are deliberately not evenly spaced: with an even spread the revenue
 * ranking would always equal the occupancy ranking times a constant, and M6's
 * report would have nothing to compute.
 *
 * @author YZ (placeholder)
 */
public enum RoomType {
    SINGLE(200.00, "Single"),
    DELUXE(350.00, "Deluxe"),
    SUITE(700.00, "Suite");

    private final double ratePerNight;
    private final String label;

    RoomType(double ratePerNight, String label) {
        this.ratePerNight = ratePerNight;
        this.label = label;
    }

    public double getRatePerNight() {
        return ratePerNight;
    }

    @Override
    public String toString() {
        return label;
    }
}
