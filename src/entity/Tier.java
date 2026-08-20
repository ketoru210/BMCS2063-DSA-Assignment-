package entity;

import java.io.Serializable;

/**
 * @author Lai Kang Yong
 */

public class Tier implements Serializable, Comparable<Tier> {
    private String season;
    private LoyaltyTier loyaltyTier;
    private int seasonalPoints;
    public Tier(String season, LoyaltyTier loyaltyTier, int seasonalPoints) {
        this.season = season;
        this.loyaltyTier = loyaltyTier;
        this.seasonalPoints = seasonalPoints;
    }
    //Accessors(Getters)
    public String getSeason() { return season; }
    public LoyaltyTier getLoyaltyTier() { return loyaltyTier; }
    public int getSeasonalPoints() { return seasonalPoints; }
    //Compare to method
    @Override
    public int compareTo(Tier tier) {
        return season.compareTo(tier.season);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return season.equals(((Tier) object).season);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+loyaltyTier+"\n--------------------\nSeason: "+season+"\nSeasonal points: "+seasonalPoints+"\n");
    }
}
