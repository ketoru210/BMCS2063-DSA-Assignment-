package entity;

import java.io.Serializable;

/* 我不晓得我要不要用这个东西，也不知道应不应该手搓。
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
*/

/**
 * @author Kang Yong
 */

public class Notification implements Serializable, Comparable<Notification> {
    private String label;
    private String message;
    private String publishedDatetime;
    private NotificationType notificationType;
    private boolean isRead;
    public Notification(String label, String message, String publishedDatetime, NotificationType notificationType) {
        this.label = label;
        this.message = message;
        this.publishedDatetime = publishedDatetime;
        this.notificationType = notificationType;
        this.isRead = false;
    }
    //Accessors(Getters)
    public String getLabel() { return label; }
    public String getMessage() { return message; }
    public String getPublishedDatetime() { return publishedDatetime; }
    public NotificationType getType() {return notificationType;}
    public boolean getIsRead() { return isRead; }
    public void read() { isRead = true; }
    //Compare to method
    @Override
    public int compareTo(Notification notification) {
        return publishedDatetime.compareTo(notification.publishedDatetime);
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
