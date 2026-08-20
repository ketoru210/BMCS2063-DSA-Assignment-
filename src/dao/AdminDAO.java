package dao;

import adt.CollectionInterface;
import adt.DoublyLinkedList;
import entity.Admin;
import java.util.Iterator;

/**
 * @author Lai Kang Yong
 */
public class AdminDAO {
    private static final CollectionInterface<Admin> ADMINS = seed();

    private static CollectionInterface<Admin> seed() {
        CollectionInterface<Admin> admins = new DoublyLinkedList<>();
        admins.add(new Admin("admin1", "pw1234", "System Administrator"));
        admins.add(new Admin("admin2", "pw1234", "Resort Manager"));
        return admins;
    }

    public CollectionInterface<Admin> getAllAdmins() {
        return ADMINS;
    }

    public Admin findByUsername(String username) {
        Iterator<Admin> walker = ADMINS.getIterator();
        while (walker.hasNext()) {
            Admin admin = walker.next();
            if (admin.getUsername().equals(username)) {
                return admin;
            }
        }
        return null;
    }
}
