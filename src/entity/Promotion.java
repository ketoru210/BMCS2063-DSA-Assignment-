package entity;

import java.io.Serializable;

/**
 * @author Kang Yong
 */

public class Promotion implements Serializable, Comparable<Promotion> {
    private String promotionID;
    private String label;
    private String description;
    private String startDate;
    private String expiryDate;
    private LoyaltyTier[] targetTiers;
    private static int promotionCount = 0;
    public Promotion(String label, String description, String startDate, String expiryDate, LoyaltyTier[] targetTiers) {
        this.promotionID = String.format("P%05d", ++promotionCount);
        this.label = label;
        this.description = description;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
        this.targetTiers = targetTiers;
    }
    //Accessors(Getters)
    public String getPromotionID() { return promotionID; }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getStartDate() { return startDate; }
    public String getExpiryDate() { return expiryDate; }
    public LoyaltyTier[] getTargetTiers() { return targetTiers; }
    public static int getPromotionCount() { return promotionCount; }
    //Mutators(Setters)
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
    public void setTargetTiers(LoyaltyTier[] targetTiers) { this.targetTiers = targetTiers; }
    //Compare to method
    @Override
    public int compareTo(Promotion promotion) {
        return promotionID.compareTo(promotion.promotionID);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Promotion other = (Promotion) object;
        return label.equals(other.label) && startDate.equals(other.startDate);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+label+"\n--------------------\n"+description+"\n\nAvailable since: "+startDate+"\nBest before: "+expiryDate+"\n");
    }
}
