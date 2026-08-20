package entity;

/**
 * @author Lai Kang Yong
 */
public enum NotificationType {
    ANNOUNCEMENT("Announcement"),
    PROMOTION("Promotion"),
    REDEMPTION("Redemption"),
    TIER_CHANGE("Tier change");
    private final String label;
    NotificationType(String label) {
        this.label = label;
    }
    @Override
    public String toString() {
        return label;
    }
}
