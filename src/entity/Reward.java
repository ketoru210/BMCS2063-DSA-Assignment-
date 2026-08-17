package entity;

import java.io.Serializable;

public class Reward implements Serializable, Comparable<Reward>{
    private String rewardID;
    private String rewardName;
    private String description;
    private int requiredPoints;
    private boolean isAvailable;
    private static int rewardCount = 0;
    public Reward(String rewardName, String description, int requiredPoints) {
        this.rewardID = String.format("RW%05d", ++rewardCount);
        this.rewardName = rewardName;
        this.description = description;
        this.requiredPoints = requiredPoints;
        isAvailable = true;
    }
    //Accessors(Getters)
    public String getRewardID() { return rewardID; }
    public String getRewardName() { return rewardName; }
    public String getDescription() { return description; }
    public int getRequiredPoints() { return requiredPoints; }
    public boolean isAvailable() { return isAvailable; }
    //Mutators(Setters)
    public void setRewardName(String rewardName) { this.rewardName = rewardName; }
    public void setDescription(String description) { this.description = description; }
    public void setRequiredPoints(int requiredPoints) { this.requiredPoints = requiredPoints; }
    public void disableReward() { this.isAvailable = false; }
    //Compare to method
    @Override
    public int compareTo(Reward reward) {
        return rewardID.compareTo(reward.rewardID);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return rewardID.equals(((Reward) object).rewardID);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+rewardName+"\n--------------------\n"+description+"\nPoints required: "+requiredPoints+"\nAvailability: "+(isAvailable?"Yes\n":"No\n"));
    }
}
