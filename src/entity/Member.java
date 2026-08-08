package entity;

import java.io.Serializable;
import adt.CollectionInterface;
import adt.DoublyLinkedList;

/**
 * @author Kang Yong
 */

public class Member implements Serializable, Comparable<Member> {
    private String username;
    private String password;
    private String memberID;
    private String name;
    private LoyaltyTier currentTier;
    private int currentPoints;
    private static int memberCount = 0;
    CollectionInterface<Tier> tierRecords = new DoublyLinkedList<>();
    CollectionInterface<Redemption> redemptionRecords = new DoublyLinkedList<>();
    CollectionInterface<Promotion> promotionRecords = new DoublyLinkedList<>();
    CollectionInterface<Notification> notificationRecords = new DoublyLinkedList<>();
    public Member(String username, String password, String name) {
        this(username, password, name, LoyaltyTier.SILVER);
    }
    // seeded members and walk-in guests do not all start at SILVER
    public Member(String username, String password, String name, LoyaltyTier currentTier) {
        this.username = username;
        this.password = password;
        this.memberID = String.format("M%05d", ++memberCount);
        this.name = name;
        this.currentTier = currentTier;
        currentPoints = 0;
    }
    //Accessors(Getters)
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getMemberID() { return memberID; }
    public String getName() { return name; }
    public LoyaltyTier getCurrentTier() { return currentTier; }
    public int getCurrentPoints() { return currentPoints; }
    public static int getMemberCount() { return memberCount; }
    public CollectionInterface<Tier> getTierRecords() { return tierRecords; }
    public CollectionInterface<Redemption> getRedemptionRecords() { return redemptionRecords; }
    public CollectionInterface<Promotion> getPromotionRecords() { return promotionRecords; }
    public CollectionInterface<Notification> getNotificationRecords() { return notificationRecords; }
    //Mutators(Setters)
    public void setCurrentTier(LoyaltyTier currentTier) { this.currentTier = currentTier; }
    public void setCurrentPoints(int currentPoints) { this.currentPoints = currentPoints; }
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
