package entity;

/**
 * @author Kang Yong
 */

public class Tier implements Comparable<Tier> {
    private enum LoyaltyTier {SILVER, GOLD, PLATINUM};
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
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+loyaltyTier+"\n--------------------\nSeason: "+season+"\nSeasonal points: "+seasonalPoints+"\n");
    }
}
