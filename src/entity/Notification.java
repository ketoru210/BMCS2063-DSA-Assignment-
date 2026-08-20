package entity;

import java.io.Serializable;

/**
 * @author Lai Kang Yong
 */

public class Notification implements Serializable, Comparable<Notification> {
    private String notificationID;
    private String label;
    private String message;
    private String publishedDatetime;
    private NotificationType notificationType;
    private boolean isRead;
    private static int notificationCount = 0;
    public Notification(String label, String message, String publishedDatetime, NotificationType notificationType) {
        this.notificationID = String.format("N%05d", ++notificationCount);
        this.label = label;
        this.message = message;
        this.publishedDatetime = publishedDatetime;
        this.notificationType = notificationType;
        this.isRead = false;
    }
    //Accessors(Getters)
    public String getNotificationID() { return notificationID; }
    public String getLabel() { return label; }
    public String getMessage() { return message; }
    public String getPublishedDatetime() { return publishedDatetime; }
    public NotificationType getType() { return notificationType; }
    public static int getNotificationCount() { return notificationCount; }
    public boolean getIsRead() { return isRead; }
    public void read() { isRead = true; }
    //Compare to method
    @Override
    public int compareTo(Notification notification) {
        return notificationID.compareTo(notification.notificationID);
    }
    //Equals method
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Notification other = (Notification) object;
        return label.equals(other.label) && publishedDatetime.equals(other.publishedDatetime);
    }
    //To string method
    @Override
    public String toString() {
        return ("--------------------\n"+label+"\n--------------------\n"+message+"\n\nPublished on: "+publishedDatetime+"\n");
    }
}
