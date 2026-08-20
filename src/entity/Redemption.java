package entity;

import java.io.Serializable;

/**
 * @author Lai Kang Yong
 */

public class Redemption implements Serializable, Comparable<Redemption> {
    private String redemptionID;
    private String label;
    private String reward;
    private int pointsSpent;
    private String redemptionDate;
    private static int redemptionCount = 0;
    public Redemption(String label, String reward, int pointsSpent, String redemptionDate) {
        this.redemptionID = String.format("RD%05d", ++redemptionCount);
        this.label = label;
        this.reward = reward;
        this.pointsSpent = pointsSpent;
        this.redemptionDate = redemptionDate;
    }
    //Accessors(Getters)
    public String getRedemptionID() { return redemptionID; }
    public String getLabel() { return label; }
    public String getReward() { return reward; }
    public int getPointsSpent() { return pointsSpent; }
    public String getRedemptionDate() { return redemptionDate; }
    public int getRedemptionCount() { return redemptionCount; }
    //Compare to method
    @Override
    public int compareTo(Redemption redemption) {
        return redemptionID.compareTo(redemption.redemptionID);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Redemption other = (Redemption) object;
        return label.equals(other.label) && redemptionDate.equals(other.redemptionDate);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+label+"\n--------------------\n"+reward+"\nPoints spent: "+pointsSpent+"\nRedeemed on: "+redemptionDate+"\n");
    }
}
