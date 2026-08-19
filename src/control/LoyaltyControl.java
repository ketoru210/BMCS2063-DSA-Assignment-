package control;

import adt.DoublyLinkedList;
import dao.AdminDAO;
import dao.MemberDAO;
import dao.RewardDAO;
import dao.TierRequirementDAO;
import entity.Admin;
import entity.LoyaltyTier;
import entity.Member;
import entity.Notification;
import entity.NotificationType;
import entity.Promotion;
import entity.Redemption;
import entity.Reward;
import entity.Tier;
import entity.TierRequirement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

/**
 * Control class for the Loyalty and Rewards Service.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Loads seed data supplied by DAO classes.</li>
 *     <li>Populates the custom ADTs.</li>
 *     <li>Performs member authentication and registration.</li>
 *     <li>Performs membership, promotion, notification and redemption logic.</li>
 *     <li>Performs administrative operations.</li>
 * </ul>
 *
 * <p>
 * DAO provides data. Control performs operations.
 *
 * @author Kang Yong
 */
public class LoyaltyControl {
    private final DoublyLinkedList<Member> memberDoublyLinkedList;
    private final MemberDAO memberDAO;
    private final AdminDAO adminDAO;
    private final RewardDAO rewardDAO;
    private final TierRequirementDAO tierRequirementDAO;
    private Reward[] rewards;
    private final TierRequirement[] tierRequirements;
    private LocalDate seasonStart;
    private LocalDate seasonEnd;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public LoyaltyControl() {
        memberDoublyLinkedList = new DoublyLinkedList<>();
        memberDAO = new MemberDAO();
        adminDAO = new AdminDAO();
        rewardDAO = new RewardDAO();
        tierRequirementDAO = new TierRequirementDAO();
        rewards = rewardDAO.getRewards();
        tierRequirements = tierRequirementDAO.getAllTierRequirements();
        loadMemberData();
        initializeSeason();
    }
    private void loadMemberData() {
        Member[] members = memberDAO.getAllMembers();
        Promotion[][] promotions = memberDAO.getAllPromotionRecords();
        Notification[][] notifications = memberDAO.getAllNotificationRecords();
        Redemption[][] redemptions = memberDAO.getAllRedemptionRecords();
        Tier[][] tiers = memberDAO.getAllTierRecords();
        for (int i = 0; i < members.length; i++) {
            Member member = members[i];
            if (!memberDoublyLinkedList.contains(member)) memberDoublyLinkedList.add(member);
            loadPromotions(member, promotions[i]);
            loadNotifications(member, notifications[i]);
            loadRedemptions(member, redemptions[i]);
            loadTierRecords(member, tiers[i]);
        }
    }
    private void loadPromotions(Member member, Promotion[] records) {
        for (Promotion promotion : records) {
            if (!member.getPromotionRecords().contains(promotion)) member.getPromotionRecords().add(promotion);
        }
    }
    private void loadNotifications(Member member, Notification[] records) {
        for (Notification notification : records) {
            if (!member.getNotificationRecords().contains(notification)) member.getNotificationRecords().add(notification);
        }
    }
    private void loadRedemptions(Member member, Redemption[] records) {
        for (Redemption redemption : records) {
            if (!member.getRedemptionRecords().contains(redemption)) member.getRedemptionRecords().add(redemption);
        }
    }
    private void loadTierRecords(Member member, Tier[] records) {
        for (Tier tier : records) {
            if (!member.getTierRecords().contains(tier)) member.getTierRecords().add(tier);
        }
    }
    private void initializeSeason() {
        LocalDate today = LocalDate.now();
        LocalDate juneEnd = LocalDate.of(today.getYear(), 6, 30);
        if (!today.isAfter(juneEnd)) {
            seasonStart = LocalDate.of(today.getYear(), 1, 1);
            seasonEnd = juneEnd;
        } else {
            seasonStart = LocalDate.of(today.getYear(), 7, 1);
            seasonEnd = LocalDate.of(today.getYear(), 12, 31);
        }
    }
    public String getSeasonSummary() {
        return seasonStart.format(DATE_FORMAT) + " to " + seasonEnd.format(DATE_FORMAT);
    }
    public Member[] getAllMembers() {
        Member[] result = new Member[memberDoublyLinkedList.size()];
        Iterator<Member> iterator = memberDoublyLinkedList.getIterator();
        int index = 0;
        while (iterator.hasNext()) {
            result[index++] = iterator.next();
        }
        return result;
    }
    public Member findMemberByUsername(String username) {
        if (username == null || username.isBlank()) return null;
        Iterator<Member> iterator = memberDoublyLinkedList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (username.equalsIgnoreCase(member.getUsername())) return member;
        }
        return null;
    }
    public Member loginMember(String username, String password) {
        if (username == null || password == null) return null;
        Iterator<Member> iterator = memberDoublyLinkedList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (username.equals(member.getUsername()) && password.equals(member.getPassword()) && member.getCurrentTier() != LoyaltyTier.GUEST) {
                refreshMemberState(member);
                return member;
            }
        }
        return null;
    }
    public Member registerMember(String username, String password, String name) {
        Member member = new Member(username, password, name, LoyaltyTier.SILVER);
        memberDoublyLinkedList.add(member);
        return member;
    }
    public void updateMemberName(Member member, String name) { member.setName(name); }
    public void updateMemberPassword(Member member, String password) { member.setPassword(password); }
    public Admin loginAdmin(String username, String password) {
        if (username == null || password == null) return null;
        Admin admin = adminDAO.findByUsername(username);
        if (admin == null) return null;
        if (!password.equals(admin.getPassword())) return null;
        return admin;
    }
    public Tier getLatestTierRecord(Member member) {
        if (member == null || member.getTierRecords().isEmpty()) return null;
        return member.getTierRecords().getLast();
    }
    public boolean adjustMemberPoints(Member member, int points) {
        if (member == null || points < 0) return false;
        member.setCurrentPoints(points);
        return true;
    }
    public boolean updateMember(Member member, String name, String password, LoyaltyTier tier, Integer points) {
        if (member == null) return false;
        if (name != null && !name.isBlank()) member.setName(name);
        if (password != null && !password.isBlank()) member.setPassword(password);
        if (tier != null) member.setCurrentTier(tier);
        if (points != null && points >= 0) {
            member.setCurrentPoints(points);
        } else {
            return points == null;
        }
        return true;
    }
    public TierRequirement findTierRequirement(LoyaltyTier tier) {
        if (tier == null) return null;
        for (TierRequirement requirement : tierRequirements) {
            if (requirement.getLoyaltyTier() == tier) return requirement;
        }
        return null;
    }
    public Tier[] getTierRecords(Member member) {
        if (member == null) return new Tier[0];
        Tier[] result = new Tier[member.getTierRecords().size()];
        Iterator<Tier> iterator = member.getTierRecords().getIterator();
        int index = 0;
        while (iterator.hasNext()) {
            result[index++] = iterator.next();
        }
        return result;
    }
    public boolean removeTierRecord(Member member, Tier tier) {
        if (member == null || tier == null) return false;
        return member.getTierRecords().remove(tier);
    }
    public boolean changeMemberTier(Member member, LoyaltyTier newTier) {
        if (member == null || newTier == null) return false;
        if (member.getCurrentTier() == newTier) return false;
        member.setCurrentTier(newTier);
        return true;
    }
    public void endSeason() {
        String seasonLabel = getSeasonSummary();
        Iterator<Member> iterator = memberDoublyLinkedList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            if (member.getCurrentTier() == LoyaltyTier.GUEST) continue;
            member.getTierRecords().add(new Tier(seasonLabel, member.getCurrentTier(), member.getSeasonalPoints()));
            LoyaltyTier newTier = determineUpgradeTier(member);
            if (newTier == member.getCurrentTier() && shouldDowngrade(member)) newTier = downgradeOneStep(member.getCurrentTier());
            if (newTier != member.getCurrentTier()) {
                member.setCurrentTier(newTier);
                addNotification(member, "Tier Update", "Your loyalty tier is now " + newTier + ".", NotificationType.TIER_CHANGE);
            }
            member.setSeasonalPoints(0);
        }
        seasonStart = seasonEnd.plusDays(1);
        LocalDate juneEnd = LocalDate.of(seasonStart.getYear(), 6, 30);
        seasonEnd = !seasonStart.isAfter(juneEnd) ? juneEnd : LocalDate.of(seasonStart.getYear(), 12, 31);
    }
    public LoyaltyTier determineUpgradeTier(Member member) {
        if (member == null) return null;
        LoyaltyTier currentTier = member.getCurrentTier();
        if (currentTier == LoyaltyTier.PLATINUM) return LoyaltyTier.PLATINUM;
        int points = member.getSeasonalPoints();
        LoyaltyTier nextTier = currentTier == LoyaltyTier.GUEST ? LoyaltyTier.SILVER : currentTier == LoyaltyTier.SILVER ? LoyaltyTier.GOLD : LoyaltyTier.PLATINUM;
        TierRequirement requirement = findTierRequirement(nextTier);
        if (requirement != null && requirement.getPointsToUpgradeTier() >= 0 && points >= requirement.getPointsToUpgradeTier()) return nextTier;
        return currentTier;
    }
    public boolean shouldDowngrade(Member member) {
        if (member == null || member.getCurrentTier() == LoyaltyTier.GUEST) return false;
        TierRequirement requirement = findTierRequirement(member.getCurrentTier());
        if (requirement == null || requirement.getPointsToDowngradeTier() < 0) return false;
        return member.getSeasonalPoints() < requirement.getPointsToDowngradeTier();
    }
    private LoyaltyTier downgradeOneStep(LoyaltyTier tier) {
        if (tier == LoyaltyTier.PLATINUM) return LoyaltyTier.GOLD;
        return LoyaltyTier.SILVER;
    }
    public void refreshMemberState(Member member) {
        if (member == null || member.getCurrentTier() == LoyaltyTier.GUEST) return;
        if (LocalDate.now().isAfter(seasonEnd)) endSeason();
        checkPointsExpiry(member);
    }
    private void checkPointsExpiry(Member member) {
        if (member.getPointsExpiryDate() == null) return;
        LocalDate expiry = LocalDate.parse(member.getPointsExpiryDate(), DATE_FORMAT);
        LocalDate today = LocalDate.now();
        if (!today.isBefore(expiry)) {
            if (member.getCurrentPoints() > 0) addNotification(member, "Points Expired", "Your redeemable points have expired and been reset to 0.", NotificationType.REDEMPTION);
            member.setCurrentPoints(0);
            member.setPointsExpiryDate(null);
            member.setExpiryWarningPosted(false);
        } else if (!member.isExpiryWarningPosted() && !today.isBefore(expiry.minusDays(14))) {
            addNotification(member, "Points Expiring Soon", "Your " + member.getCurrentPoints() + " redeemable points expire on " + member.getPointsExpiryDate() + ".", NotificationType.REDEMPTION);
            member.setExpiryWarningPosted(true);
        }
    }
    public Promotion[] getMemberPromotions(Member member) {
        if (member == null) return new Promotion[0];
        Promotion[] result = new Promotion[member.getPromotionRecords().size()];
        Iterator<Promotion> iterator = member.getPromotionRecords().getIterator();
        int index = 0;
        while (iterator.hasNext()) {
            result[index++] = iterator.next();
        }
        return result;
    }
    public Promotion[] getGuestPromotions() {
        Promotion[] temp = new Promotion[0];
        for (Promotion promotion : memberDAO.getAllPromotions()) {
            LoyaltyTier[] targetTiers = promotion.getTargetTiers();
            for (LoyaltyTier tier : targetTiers) {
                if (tier == LoyaltyTier.GUEST) {
                    Promotion[] result = new Promotion[temp.length + 1];
                    System.arraycopy(temp, 0, result, 0, temp.length);
                    result[temp.length] = promotion;
                    temp = result;
                    break;
                }
            }
        }
        return temp;
    }
    public Promotion findPromotionByID(Member member, int id) {
        if (member == null) return null;
        String promotionID = String.format("P%05d", id);
        Iterator<Promotion> iterator = member.getPromotionRecords().getIterator();
        while (iterator.hasNext()) {
            Promotion promotion = iterator.next();
            if (promotion.getPromotionID().equals(promotionID)) return promotion;
        }
        return null;
    }
    public Promotion findPromotionByLabel(Member member, String label) {
        if (member == null || label == null) return null;
        Iterator<Promotion> iterator = member.getPromotionRecords().getIterator();
        while (iterator.hasNext()) {
            Promotion promotion = iterator.next();
            if (promotion.getLabel().equalsIgnoreCase(label)) return promotion;
        }
        return null;
    }
    public void assignPromotion(Member member, Promotion promotion) {
        if (member == null || promotion == null) return;
        if (member.getPromotionRecords().contains(promotion)) return;
        member.getPromotionRecords().add(promotion);
    }
    public boolean removePromotion(Member member, Promotion promotion) {
        if (member == null || promotion == null) return false;
        return member.getPromotionRecords().remove(promotion);
    }
    public Notification[] getGuestNotifications() {
        return memberDAO.getGuestNotifications();
    }
    public Notification[] getMemberNotifications(Member member) {
        if (member == null) return new Notification[0];
        Notification[] result = new Notification[member.getNotificationRecords().size()];
        Iterator<Notification> iterator = member.getNotificationRecords().getIterator();
        int index = 0;
        while (iterator.hasNext()) {
            result[index++] = iterator.next();
        }
        return result;
    }
    public Notification findNotificationByID(Member member, int id) {
        if (member == null) return null;
        String notificationID = String.format("N%05d", id);
        Iterator<Notification> iterator = member.getNotificationRecords().getIterator();
        while (iterator.hasNext()) {
            Notification notification = iterator.next();
            if (notification.getNotificationID().equals(notificationID)) return notification;
        }
        return null;
    }
    public int countUnreadNotifications(Member member) {
        if (member == null) return 0;
        int count = 0;
        Iterator<Notification> iterator = member.getNotificationRecords().getIterator();
        while (iterator.hasNext()) {
            if (!iterator.next().getIsRead()) {
                count++;
            }
        }
        return count;
    }
    public void addNotification(Member member, String label, String message, NotificationType type) {
        if (member == null || label == null || label.isBlank() || message == null || message.isBlank() || type == null) return;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Notification notification = new Notification(label, message, date, type);
        member.getNotificationRecords().add(notification);
    }
    public boolean removeNotification(Member member, Notification notification) {
        if (member == null || notification == null) return false;
        return member.getNotificationRecords().remove(notification);
    }
    public Reward[] getAvailableRewards() { return rewards; }
    public Redemption[] getRedemptionRecords(Member member) {
        if (member == null) return new Redemption[0];
        Redemption[] result = new Redemption[member.getRedemptionRecords().size()];
        Iterator<Redemption> iterator = member.getRedemptionRecords().getIterator();
        int index = 0;
        while (iterator.hasNext()) {
            result[index++] = iterator.next();
        }
        return result;
    }
    public Reward findRewardByID(int id) {
        String rewardID = String.format("RW%05d", id);
        for (Reward reward : rewards) {
            if (reward.getRewardID().equals(rewardID)) return reward;
        }
        return null;
    }
    public Reward findRewardByName(String name) {
        if (name == null) return null;
        for (Reward reward : rewards) {
            if (reward.getRewardName().equalsIgnoreCase(name)) return reward;
        }
        return null;
    }
    public boolean earnPoints(Member member, int amountSpent) {
        if (member == null || amountSpent <= 0 || member.getCurrentTier() == LoyaltyTier.GUEST) return false;
        TierRequirement requirement = findTierRequirement(member.getCurrentTier());
        int multiplier = requirement != null ? requirement.getPointMultiplier() : 1;
        member.setCurrentPoints(member.getCurrentPoints() + amountSpent * multiplier);
        member.addSeasonalPoints(amountSpent);
        member.setPointsExpiryDate(LocalDate.now().plusMonths(6).format(DATE_FORMAT));
        member.setExpiryWarningPosted(false);
        return true;
    }
    public boolean redeemReward(Member member, Reward reward) {
        if (member == null || reward == null || !reward.isAvailable()) return false;
        int requiredPoints = reward.getRequiredPoints();
        if (member.getCurrentPoints() < requiredPoints) return false;
        member.setCurrentPoints(member.getCurrentPoints() - requiredPoints);
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        Redemption redemption = new Redemption(reward.getRewardName(), reward.getRewardName(), requiredPoints, date);
        member.getRedemptionRecords().add(redemption);
        addNotification(member, "Redemption Successful", "Your " + reward.getRewardName() + " has been successfully redeemed.", NotificationType.REDEMPTION);
        return true;
    }
    public boolean removeRedemption(Member member, Redemption redemption) {
        if (member == null || redemption == null) return false;
        return member.getRedemptionRecords().remove(redemption);
    }
    public Reward createReward(String name, String description, int points) {
        if (name == null || name.isBlank() || points < 0) return null;
        Reward reward = new Reward(name, description, points);
        Reward[] updated = new Reward[rewards.length + 1];
        System.arraycopy(rewards, 0, updated, 0, rewards.length);
        updated[rewards.length] = reward;
        rewards = updated;
        return reward;
    }
    public Promotion[] getAllPromotions() { return memberDAO.getAllPromotions(); }
    public Promotion createPromotion(String label, String description, String startDate, String expiryDate, LoyaltyTier[] targetTiers) {
        if (label == null || label.isBlank() || description == null || description.isBlank() || startDate == null || startDate.isBlank() || expiryDate == null || expiryDate.isBlank() || targetTiers == null || targetTiers.length == 0) return null;
        return new Promotion(label, description, startDate, expiryDate, targetTiers);
    }
    public boolean disableReward(int id) {
        Reward reward = findRewardByID(id);
        if (reward == null || !reward.isAvailable()) return false;
        reward.disableReward();
        return true;
    }
    public boolean updateTierRequirement(LoyaltyTier tier, int updateRequirement, int maintenanceRequirement, String benefits) {
        if (tier == null) return false;
        TierRequirement requirement = findTierRequirement(tier);
        if (requirement == null) return false;
        if (updateRequirement != -1) requirement.changePointsToUpgradeTier(updateRequirement);
        if (maintenanceRequirement != -1) requirement.changePointsToDowngradeTier(maintenanceRequirement);
        if (!benefits.isEmpty())requirement.changeBenefits(benefits);
        return true;
    }
    public void postAnnouncement(String label, String message) {
        Iterator<Member> iterator = memberDoublyLinkedList.getIterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            addNotification(member, label, message, NotificationType.ANNOUNCEMENT);
        }
    }
    public void postPromotionNotification(Member member, Promotion promotion) {
        if (member == null || promotion == null) return;
        addNotification(member, promotion.getLabel(), promotion.getDescription(), NotificationType.PROMOTION);
    }
    public DoublyLinkedList<Tier>.Cursor getTierCursor(Member member, int startIndex) {
        DoublyLinkedList<Tier>.Cursor cursor = member.getTierCursor();
        for (int i = 0; i < startIndex; i++) cursor.next();
        return cursor;
    }
    public DoublyLinkedList<Redemption>.Cursor getRedemptionCursor(Member member, int startIndex) {
        DoublyLinkedList<Redemption>.Cursor cursor = member.getRedemptionCursor();
        for (int i = 0; i < startIndex; i++) cursor.next();
        return cursor;
    }
    public DoublyLinkedList<Promotion>.Cursor getPromotionCursor(Member member, int startIndex) {
        DoublyLinkedList<Promotion>.Cursor cursor = member.getPromotionCursor();
        for (int i = 0; i < startIndex; i++) cursor.next();
        return cursor;
    }
    public DoublyLinkedList<Notification>.Cursor getNotificationCursor(Member member, int startIndex) {
        DoublyLinkedList<Notification>.Cursor cursor = member.getNotificationCursor();
        for (int i = 0; i < startIndex; i++) cursor.next();
        return cursor;
    }
}