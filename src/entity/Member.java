package entity;

import java.io.Serializable;
import adt.CollectionInterface;
import adt.DoublyLinkedList;

/**
 * @author Lai Kang Yong
 */

public class Member implements Serializable, Comparable<Member> {
    private String memberID;
    private String username;
    private String password;
    private String name;
    private LoyaltyTier currentTier;
    private int currentPoints;
    private int seasonalPoints;
    private String pointsExpiryDate;
    private boolean expiryWarningPosted;
    private static int memberCount = 0;
    CollectionInterface<Notification> notificationRecords;
    CollectionInterface<Promotion> promotionRecords;
    CollectionInterface<Redemption> redemptionRecords;
    CollectionInterface<Tier> tierRecords;
    public Member(String username, String password, String name) {
        this(username, password, name, LoyaltyTier.SILVER);
    }
    // seeded members and walk-in guests do not all start at SILVER
    public Member(String username, String password, String name, LoyaltyTier currentTier) {
        this.memberID = String.format("M%05d", ++memberCount);
        this.username = username;
        this.password = password;
        this.name = name;
        this.currentTier = currentTier;
        this.currentPoints = 0;
        this.seasonalPoints = 0;
        this.pointsExpiryDate = null;
        this.expiryWarningPosted = false;
        this.notificationRecords = new DoublyLinkedList<>();
        this.promotionRecords = new DoublyLinkedList<>();
        this.redemptionRecords = new DoublyLinkedList<>();
        this.tierRecords = new DoublyLinkedList<>();
    }
    //Accessors(Getters)
    public String getMemberID() { return memberID; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public LoyaltyTier getCurrentTier() { return currentTier; }
    public int getCurrentPoints() { return currentPoints; }
    public static int getMemberCount() { return memberCount; }
    public int getSeasonalPoints() { return seasonalPoints; }
    public String getPointsExpiryDate() { return pointsExpiryDate; }
    public boolean isExpiryWarningPosted() { return expiryWarningPosted; }
    public CollectionInterface<Notification> getNotificationRecords() { return notificationRecords; }
    public CollectionInterface<Promotion> getPromotionRecords() { return promotionRecords; }
    public CollectionInterface<Redemption> getRedemptionRecords() { return redemptionRecords; }
    public CollectionInterface<Tier> getTierRecords() { return tierRecords; }
    //Mutators(Setters)
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }
    public void setCurrentTier(LoyaltyTier currentTier) { this.currentTier = currentTier; }
    public void setCurrentPoints(int currentPoints) { this.currentPoints = currentPoints; }
    public void setSeasonalPoints(int seasonalPoints) { this.seasonalPoints = seasonalPoints; }
    public void addSeasonalPoints(int delta) { this.seasonalPoints += delta; }
    public void setPointsExpiryDate(String pointsExpiryDate) { this.pointsExpiryDate = pointsExpiryDate; }
    public void setExpiryWarningPosted(boolean expiryWarningPosted) { this.expiryWarningPosted = expiryWarningPosted; }
    public DoublyLinkedList<Notification>.Cursor getNotificationCursor() {
        return ((DoublyLinkedList<Notification>) notificationRecords).getCursor();
    }
    public DoublyLinkedList<Promotion>.Cursor getPromotionCursor() {
        return ((DoublyLinkedList<Promotion>) promotionRecords).getCursor();
    }
    public DoublyLinkedList<Redemption>.Cursor getRedemptionCursor() {
        return ((DoublyLinkedList<Redemption>) redemptionRecords).getCursor();
    }
    public DoublyLinkedList<Tier>.Cursor getTierCursor() {
        return ((DoublyLinkedList<Tier>) tierRecords).getCursor();
    }
    //Compare to method
    @Override
    public int compareTo(Member member) {
        return username.compareTo(member.username);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return memberID.equals(((Member) object).memberID);
    }
    //To string method
    @Override
    public String toString() {
        return String.format("%s | %-18s | %-9s | %d pts", memberID, name, currentTier, currentPoints);
    }
}
