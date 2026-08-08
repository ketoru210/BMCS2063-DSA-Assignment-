package entity;

import java.io.Serializable;

/**
 * @author Kang Yong
 */

public class Tier implements Serializable, Comparable<Tier> {
    private LoyaltyTier loyaltyTier;
    private int season;
    private int seasonalPoints;
    public Tier(LoyaltyTier loyaltyTier, int season, int seasonalPoints) {
        this.loyaltyTier = loyaltyTier;
        this.season = season;
        this.seasonalPoints = seasonalPoints;
    }
    //Accessors(Getters)
    public LoyaltyTier getLoyaltyTier() { return loyaltyTier; }
    public int getSeason() { return season; }
    public int getSeasonalPoints() { return seasonalPoints; }
    //Compare to method
    @Override
    public int compareTo(Tier tier) {
        return Integer.compare(season, tier.season);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return season == ((Tier) object).season;
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+loyaltyTier+"\n--------------------\nSeason: "+season+"\nSeasonal points: "+seasonalPoints+"\n");
    }
}
