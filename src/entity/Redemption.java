package entity;

/**
 * @author Kang Yong
 */

public class Redemption implements Comparable<Redemption> {
    private String label;
    private String reward;
    private int pointsSpent;
    private String redemptionDate;
    public Redemption(String label, String reward, int pointsSpent, String redemptionDate) {
        this.label = label;
        this.reward = reward;
        this.pointsSpent = pointsSpent;
        this.redemptionDate = redemptionDate;
    }
    //Accessors(Getters)
    public String getLabel() { return label; }
    public String getReward() { return reward; }
    public int getPointsSpent() { return pointsSpent; }
    public String getRedemptionDate() { return redemptionDate; }
    //Compare to method
    @Override
    public int compareTo(Redemption redemption) {
        return redemptionDate.compareTo(redemption.redemptionDate);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+label+"\n--------------------\n"+reward+"\nPoints spent: "+pointsSpent+"\nRedeemed on: "+redemptionDate+"\n");
    }
}
