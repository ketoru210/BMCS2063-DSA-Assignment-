package dao;

import entity.Admin;

public class AdminDAO {
    private static final Admin[] ADMINS = {
            new Admin("admin1", "pw1234", "System Administrator"),
            new Admin("admin2", "pw1234", "Resort Manager")
    };
    public Admin[] getAllAdmins() {
        return ADMINS;
    }
    public Admin findByUsername(String username) {
        for (Admin admin : ADMINS) {
            if (admin.getUsername().equals(username)) {
                return admin;
            }
        }
        return null;
    }
}