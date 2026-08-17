package dao;

import entity.LoyaltyTier;
import entity.Member;
import entity.Notification;
import entity.NotificationType;
import entity.Promotion;
import entity.Redemption;
import entity.Tier;

/**
 * Hard-coded member registry (M1 was dropped; the spec permits seeding entity
 * values to RAM).
 * <p>
 * Members are looked up by username, not by memberID: Member generates the ID
 * from a static counter, so the value depends on construction order and is not
 * safe to hard-code here.
 *
 * @author YZ
 * Seed data source for the Loyalty and Rewards Service.
 *
 * <p>MemberDAO provides the initial members and their member-specific records.
 * The DAO only provides data. LoyaltyControl is responsible for loading these
 * arrays into each Member's corresponding ADTs.</p>
 *
 * @author Kang Yong
 */

public class MemberDAO {
    /** One member's full seed bundle — keeps everything about a member in one place. */
    private static final class MemberSeed {
        final Member member;
        final Promotion[] promotions;
        final Notification[] notifications;
        final Redemption[] redemptions;
        final Tier[] tiers;
        MemberSeed(Member member, Promotion[] promotions, Notification[] notifications, Redemption[] redemptions, Tier[] tiers) {
            this.member = member;
            this.promotions = promotions;
            this.notifications = notifications;
            this.redemptions = redemptions;
            this.tiers = tiers;
        }
    }

    private static final Promotion WELCOME_BACK = new Promotion("Welcome Back", "Enjoy 10% off your next room booking.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.SILVER, LoyaltyTier.GOLD, LoyaltyTier.PLATINUM});
    private static final Promotion SILVER_DINING = new Promotion("Silver Dining Deal", "Silver members receive 10% off selected dining outlets.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.SILVER});
    private static final Promotion GOLD_DINING = new Promotion("Gold Dining Deal", "Gold members receive 20% off selected dining outlets.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.GOLD});
    private static final Promotion PLATINUM_DINING = new Promotion("Platinum Dining Deal", "Platinum members receive 30% off selected dining outlets.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.PLATINUM});
    private static final Promotion WEEKEND_ESCAPE = new Promotion("Weekend Escape", "Enjoy 15% off weekend room bookings.", "01-03-2026", "30-09-2026", new LoyaltyTier[]{LoyaltyTier.SILVER, LoyaltyTier.GOLD, LoyaltyTier.PLATINUM});
    private static final Promotion GOLD_SPA = new Promotion("Gold Spa Privilege", "Gold members receive 20% off selected spa treatments.", "01-02-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.GOLD});
    private static final Promotion PLATINUM_SPA = new Promotion("Platinum Spa Privilege", "Platinum members receive 40% off selected spa treatments.", "01-02-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.PLATINUM});
    private static final Promotion COMPLIMENTARY_BREAKFAST = new Promotion("Complimentary Breakfast", "Gold and Platinum members enjoy complimentary breakfast during their stay.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.GOLD, LoyaltyTier.PLATINUM});
    private static final Promotion PLATINUM_ROOM_UPGRADE = new Promotion("Platinum Room Upgrade", "Platinum members receive a complimentary room upgrade, subject to availability.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.PLATINUM});
    private static final Promotion BIRTHDAY_CELEBRATION = new Promotion("Birthday Celebration", "Enjoy a special dining benefit during your birthday month.", "01-01-2026", "31-12-2026", new LoyaltyTier[]{LoyaltyTier.SILVER, LoyaltyTier.GOLD, LoyaltyTier.PLATINUM});

    private static final Promotion[] PROMOTIONS = {
            WELCOME_BACK, SILVER_DINING, GOLD_DINING, PLATINUM_DINING, WEEKEND_ESCAPE,
            GOLD_SPA, PLATINUM_SPA, COMPLIMENTARY_BREAKFAST, PLATINUM_ROOM_UPGRADE, BIRTHDAY_CELEBRATION
    };

    private static final MemberSeed[] SEEDS = {
            new MemberSeed(
                    new Member("tanwm", "pw1234", "Tan Wei Ming", LoyaltyTier.PLATINUM),
                    new Promotion[]{WELCOME_BACK, PLATINUM_DINING, PLATINUM_SPA, COMPLIMENTARY_BREAKFAST, PLATINUM_ROOM_UPGRADE, BIRTHDAY_CELEBRATION},
                    new Notification[]{
                            new Notification("Platinum Tier Achieved", "Congratulations! You have reached Platinum tier.", "01-01-2026", NotificationType.TIER_CHANGE),
                            new Notification("Redemption Successful", "Your RM100 Dining Voucher has been successfully redeemed.", "10-02-2026", NotificationType.REDEMPTION),
                            new Notification("Festive Season Event", "Join us for our special festive season event.", "15-02-2026", NotificationType.ANNOUNCEMENT),
                            new Notification("Spa Redemption Successful", "Your spa reward redemption has been successfully processed.", "20-02-2026", NotificationType.REDEMPTION),
                            new Notification("Platinum Spa Privilege", "You can now enjoy 40% off selected spa treatments.", "01-03-2026", NotificationType.PROMOTION),
                            new Notification("Swimming Pool Closure", "The swimming pool will be temporarily closed for maintenance.", "10-03-2026", NotificationType.ANNOUNCEMENT)
                    },
                    new Redemption[]{
                            new Redemption("Dining Voucher", "RM100 Dining Voucher", 1800, "10-02-2026"),
                            new Redemption("Spa Voucher", "RM60 Spa Voucher", 1400, "20-02-2026")
                    },
                    new Tier[]{ new Tier("01-01-2026 to 30-06-2026", LoyaltyTier.PLATINUM, 15000) }
            ),
            new MemberSeed(
                    new Member("nurula", "pw1234", "Nurul Aina", LoyaltyTier.GOLD),
                    new Promotion[]{WELCOME_BACK, GOLD_DINING, GOLD_SPA, COMPLIMENTARY_BREAKFAST, BIRTHDAY_CELEBRATION},
                    new Notification[]{
                            new Notification("Gold Tier Achieved", "Congratulations! You have reached Gold tier.", "05-01-2026", NotificationType.TIER_CHANGE),
                            new Notification("Complimentary Breakfast", "You are eligible for complimentary breakfast during your stay.", "10-01-2026", NotificationType.PROMOTION),
                            new Notification("Festive Season Event", "Join us for our special festive season event.", "15-02-2026", NotificationType.ANNOUNCEMENT)
                    },
                    new Redemption[]{ new Redemption("Breakfast", "Complimentary Breakfast", 750, "12-02-2026") },
                    new Tier[]{ new Tier("01-01-2026 to 30-06-2026", LoyaltyTier.GOLD, 7000) }
            ),
            new MemberSeed(
                    new Member("limkx", "pw1234", "Lim Kai Xin", LoyaltyTier.GOLD),
                    new Promotion[]{WELCOME_BACK, GOLD_DINING, GOLD_SPA, COMPLIMENTARY_BREAKFAST},
                    new Notification[]{
                            new Notification("Gold Tier Benefits", "Your Gold membership benefits are now active.", "01-01-2026", NotificationType.TIER_CHANGE),
                            new Notification("Gold Dining Deal", "You can now enjoy 20% off selected dining outlets.", "05-01-2026", NotificationType.PROMOTION)
                    },
                    new Redemption[]{},
                    new Tier[]{ new Tier("01-01-2026 to 30-06-2026", LoyaltyTier.GOLD, 6000) }
            ),
            new MemberSeed(
                    new Member("ravic", "pw1234", "Ravi Chandran", LoyaltyTier.SILVER),
                    new Promotion[]{WELCOME_BACK, SILVER_DINING, WEEKEND_ESCAPE, BIRTHDAY_CELEBRATION},
                    new Notification[]{
                            new Notification("Welcome to Silver", "Welcome to the Silver membership tier.", "01-01-2026", NotificationType.TIER_CHANGE),
                            new Notification("Weekend Escape", "Enjoy 15% off weekend room bookings.", "01-03-2026", NotificationType.PROMOTION)
                    },
                    new Redemption[]{ new Redemption("Welcome Drink", "Welcome Drink", 300, "15-02-2026") },
                    new Tier[]{ new Tier("01-01-2026 to 30-06-2026", LoyaltyTier.SILVER, 2500) }
            ),
            new MemberSeed(
                    new Member("sitiz", "pw1234", "Siti Zubaidah", LoyaltyTier.SILVER),
                    new Promotion[]{WELCOME_BACK, SILVER_DINING, BIRTHDAY_CELEBRATION},
                    new Notification[]{ new Notification("Welcome to Silver", "Welcome to the Silver membership tier.", "01-01-2026", NotificationType.TIER_CHANGE) },
                    new Redemption[]{},
                    new Tier[]{ new Tier("01-01-2026 to 30-06-2026", LoyaltyTier.SILVER, 1800) }
            ),
            new MemberSeed(
                    new Member("chongml", "pw1234", "Chong Mei Ling", LoyaltyTier.GUEST),
                    new Promotion[]{},
                    new Notification[]{ new Notification("Swimming Pool Closure", "The swimming pool will be temporarily closed for maintenance.", "10-03-2026", NotificationType.ANNOUNCEMENT) },
                    new Redemption[]{},
                    new Tier[]{}
            ),
            new MemberSeed(
                    new Member("arunk", "pw1234", "Arun Kumar", LoyaltyTier.GUEST),
                    new Promotion[]{},
                    new Notification[]{ new Notification("Festive Season Event", "Join us for our special festive season event.", "15-02-2026", NotificationType.ANNOUNCEMENT) },
                    new Redemption[]{},
                    new Tier[]{}
            )
    };

    public Member[] getAllMembers() {
        Member[] result = new Member[SEEDS.length];
        for (int i = 0; i < SEEDS.length; i++) result[i] = SEEDS[i].member;
        return result;
    }
    public Member findByUsername(String username) {
        if (username == null) return null;
        for (MemberSeed seed : SEEDS) if (seed.member.getUsername().equals(username)) return seed.member;
        return null;
    }
    public Promotion[] getAllPromotions() { return PROMOTIONS; }
    public Notification[] getGuestNotifications() {
        return new Notification[]{ SEEDS[5].notifications[0], SEEDS[6].notifications[0] };
    }
    public Promotion[][] getAllPromotionRecords() {
        Promotion[][] result = new Promotion[SEEDS.length][];
        for (int i = 0; i < SEEDS.length; i++) result[i] = SEEDS[i].promotions;
        return result;
    }
    public Notification[][] getAllNotificationRecords() {
        Notification[][] result = new Notification[SEEDS.length][];
        for (int i = 0; i < SEEDS.length; i++) result[i] = SEEDS[i].notifications;
        return result;
    }
    public Redemption[][] getAllRedemptionRecords() {
        Redemption[][] result = new Redemption[SEEDS.length][];
        for (int i = 0; i < SEEDS.length; i++) result[i] = SEEDS[i].redemptions;
        return result;
    }
    public Tier[][] getAllTierRecords() {
        Tier[][] result = new Tier[SEEDS.length][];
        for (int i = 0; i < SEEDS.length; i++) result[i] = SEEDS[i].tiers;
        return result;
    }
}