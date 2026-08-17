package entity;

import java.io.Serializable;

public class Admin implements Serializable, Comparable<Admin> {
    private String adminID;
    private String username;
    private String password;
    private String name;
    private static int adminCount = 0;
    public Admin(String username, String password, String name) {
        this.adminID = String.format("A%05d", ++adminCount);
        this.username = username;
        this.password = password;
        this.name = name;
    }
    //Accessors(Getters)
    public String getAdminID() { return adminID; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    //Mutators(Setters)
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }
    @Override
    public int compareTo(Admin admin) {
        return username.compareTo(admin.username);
    }
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return adminID.equals(((Admin) object).adminID);
    }
    @Override
    public String toString() {
        return String.format("%s | %s (%s)", adminID, name, username);
    }
}