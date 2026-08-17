package entity;

import java.io.Serializable;

public class TierRequirement implements Serializable, Comparable<TierRequirement> {
    private LoyaltyTier loyaltyTier;
    private int pointsToDowngradeTier;
    private int pointsToUpgradeTier;
    private String benefits;
    private int pointMultiplier;
    public TierRequirement(LoyaltyTier loyaltyTier, int pointsToDowngradeTier, int pointsToUpgradeTier, String benefits, int pointMultiplier) {
        this.loyaltyTier = loyaltyTier;
        this.pointsToDowngradeTier = pointsToDowngradeTier;
        this.pointsToUpgradeTier = pointsToUpgradeTier;
        this.benefits = benefits;
        this.pointMultiplier = pointMultiplier;
    }
    //Accessors(Getters)
    public LoyaltyTier getLoyaltyTier() { return loyaltyTier; }
    public int getPointsToDowngradeTier() { return pointsToDowngradeTier; }
    public int getPointsToUpgradeTier() { return pointsToUpgradeTier; }
    public String getBenefits() { return benefits; }
    public int getPointMultiplier() { return pointMultiplier; }
    //Mutators(Setters)
    public void changePointsToDowngradeTier(int pointsToDowngradeTier) { this.pointsToDowngradeTier = pointsToDowngradeTier; }
    public void changePointsToUpgradeTier(int pointsToUpgradeTier) { this.pointsToUpgradeTier = pointsToUpgradeTier; }
    public void changeBenefits(String benefits) { this.benefits = benefits; }
    public void changePointMultiplier(int pointMultiplier) { this.pointMultiplier = pointMultiplier; }
    //Compare to method
    @Override
    public int compareTo(TierRequirement tierRequirement) {
        return loyaltyTier.compareTo(tierRequirement.loyaltyTier);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return loyaltyTier.equals(((TierRequirement) object).loyaltyTier);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+loyaltyTier+"\n--------------------\nUpgrade Requirement: "+pointsToUpgradeTier+"\nMaintenance Requirement: "+pointsToDowngradeTier +"\nBenefits: "+benefits);
    }
}
