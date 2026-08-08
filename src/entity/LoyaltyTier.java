package entity;

/**
 * Loyalty tier, shared by M5 (progression) and M2 (allocation priority).
 * <p>
 * The weight is denominated in MINUTES of waiting: one tier step is worth
 * 30 minutes under M2's aging rate k = 1.
 * <p>
 * GUEST is the bottom band, so a non-member is an ordinary Member whose tier
 * happens to be GUEST — one entity, and no module has to handle a null.
 *
 * @author YZ
 */
public enum LoyaltyTier {
    GUEST(0, "Guest"),
    SILVER(30, "Silver"),
    GOLD(60, "Gold"),
    PLATINUM(90, "Platinum");

    private final int weight;
    private final String label;

    LoyaltyTier(int weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isMember() {
        return this != GUEST;
    }

    @Override
    public String toString() {
        return label;
    }
}
