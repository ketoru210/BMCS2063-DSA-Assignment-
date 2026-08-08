package entity;

import java.io.Serializable;

/**
 * @author Kang Yong
 */

public class Promotion implements Serializable, Comparable<Promotion> {
    private String label;
    private String description;
    private String startDate;
    private String expiryDate;
    public Promotion(String label, String description, String startDate, String expiryDate) {
        this.label = label;
        this.description = description;
        this.startDate = startDate;
        this.expiryDate = expiryDate;
    }
    //Accessors(Getters)
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public String getStartDate() { return startDate; }
    public String getExpiryDate() { return expiryDate; }
    //Compare to method
    @Override
    public int compareTo(Promotion promotion) {
        return startDate.compareTo(promotion.startDate);
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
