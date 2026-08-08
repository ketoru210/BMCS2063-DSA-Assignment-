package dao;

import entity.LoyaltyTier;
import entity.Member;

/**
 * Hard-coded member registry (M1 was dropped; the spec permits seeding entity
 * values to RAM).
 * <p>
 * Members are looked up by username, not by memberID: Member generates the ID
 * from a static counter, so the value depends on construction order and is not
 * safe to hard-code here.
 *
 * @author YZ
 */
public class MemberDAO {
    // static so every control seeded from here shares one set of objects
    private static final Member[] MEMBERS = {
        new Member("tanwm", "pw1234", "Tan Wei Ming", LoyaltyTier.PLATINUM),
        new Member("nurula", "pw1234", "Nurul Aina", LoyaltyTier.GOLD),
        new Member("limkx", "pw1234", "Lim Kai Xin", LoyaltyTier.GOLD),
        new Member("ravic", "pw1234", "Ravi Chandran", LoyaltyTier.SILVER),
        new Member("sitiz", "pw1234", "Siti Zubaidah", LoyaltyTier.SILVER),
        new Member("chongml", "pw1234", "Chong Mei Ling", LoyaltyTier.GUEST),
        new Member("arunk", "pw1234", "Arun Kumar", LoyaltyTier.GUEST)
    };

    public Member[] getAllMembers() {
        return MEMBERS;
    }

    public Member findByUsername(String username) {
        for (int i = 0; i < MEMBERS.length; i++) {
            if (MEMBERS[i].getUsername().equals(username)) {
                return MEMBERS[i];
            }
        }
        return null;
    }
}
