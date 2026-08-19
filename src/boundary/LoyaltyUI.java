package boundary;

import adt.DoublyLinkedList;
import control.LoyaltyControl;
import entity.Admin;
import entity.LoyaltyTier;
import entity.Member;
import entity.Notification;
import entity.Promotion;
import entity.Redemption;
import entity.Reward;
import entity.Tier;
import entity.TierRequirement;
import utility.InputHelper;
import utility.Menu;
import utility.MenuItem;
import utility.OutputHelper;

public class LoyaltyUI {
    private static final String TITLE = "Loyalty and Rewards Service";
    private final LoyaltyControl control;
    private Member currentMember = null;
    private Admin currentAdmin = null;
    public LoyaltyUI(LoyaltyControl control) { this.control = control; }
    private enum MenuOption implements MenuItem {
        BACK("Back to Main Menu"),
        SELECT("Select One of Them"),
        PREVIOUS("Previous"),
        NEXT("Next"),
        DELETE("Delete"),

        ADMIN_LOGIN("Admin Login"),
        MEMBER_LOGIN("Member Login"),
        GUEST_LOGIN("Guest Login"),
        REGISTER("Member Register"),
        LOGOUT("Logout"),
        PROFILE("Manage Profile"),
        STATUS("Membership Status"),
        REDEMPTION("Redeem Rewards"),
        PROMOTION("Promotions"),
        NOTIFICATION("Notifications"),
        EARN_POINTS("Simulate Stay (Earn Points)"), //Just a test.

        CHANGE_NAME("Change Name"),
        CHANGE_PASSWORD("Change Password"),
        REQUIREMENTS("View All Tier Requirements"),
        TIER_HISTORY("View Tier History"),
        REWARDS("View Available Rewards"),
        REDEMPTION_HISTORY("View Redemption History"),

        MANAGE_MEMBERS("Manage Members"),
        MANAGE_PROMOTIONS("Manage Promotions"),
        MANAGE_REWARDS("Manage Rewards"),
        MANAGE_TIERS("Manage Tier Requirements"),
        MANAGE_NOTIFICATIONS("Manage Notifications"),
        MANAGE_REDEMPTIONS("View Redemption Records"),

        MODIFY_PROMOTION("Modify Existing Promotions"),
        CREATE_PROMOTION("Create Promotion"),
        VIEW_REWARD("View All Reward"),
        EDIT_REWARD("Edit Reward"),
        DISABLE_REWARD("Disable Reward"),
        ADD_REWARD("Add Reward"),
        EDIT_TIER_REQUIREMENTS("Edit a Tier Requirements"),
        END_SEASON("End Current Season Now"),
        VIEW_MEMBER_NOTIFICATION("View Member Notification"),
        ANNOUNCE("Make Announcement");
        private final String label;
        MenuOption(String label) { this.label = label; }
        @Override
        public String label() { return label; }
        @Override
        public void run() {}
    }
    public void run() {
        for (;;) {
            MenuOption[] loginOptions = {
                    MenuOption.BACK,
                    MenuOption.ADMIN_LOGIN,
                    MenuOption.MEMBER_LOGIN,
                    MenuOption.GUEST_LOGIN,
                    MenuOption.REGISTER
            };
            MenuOption selected = Menu.prompt(TITLE, "Welcome!", loginOptions);
            if (selected == MenuOption.BACK) return;
            switch (selected) {
                case ADMIN_LOGIN:
                    if (adminLoginUI()) {
                        adminMenu();
                    } else {
                        OutputHelper.printErr("Failed to log in. Please try again later.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case MEMBER_LOGIN:
                    if (memberLoginUI()) {
                        memberMenu();
                    } else {
                        OutputHelper.printErr("Failed to log in. Please try again later.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case GUEST_LOGIN:
                    guestMenu();
                    break;
                case REGISTER:
                    if (registerUI()) {
                        memberMenu();
                    } else {
                        OutputHelper.printErr("Failed to register. Please try again later.");
                        InputHelper.waitForEnter();
                    }
                    break;
                default:
                    break;
            }
        }
    }
    private boolean memberLoginUI() {
        for (;;) {
            System.out.println("\n=== Member Login ===");
            String username = InputHelper.readLine("Username (or 0 to cancel): ");
            if (username.equals("0")) return false;
            String password = InputHelper.readLine("Password (or 0 to cancel): ");
            if (password.equals("0")) return false;
            Member member = control.loginMember(username, password);
            if (member != null) {
                currentMember = member;
                OutputHelper.printOK("Login successful.");
                InputHelper.waitForEnter();
                return true;
            }
            OutputHelper.printErr("Invalid username or password.");
            System.out.println("Please try again.");
        }
    }
    private boolean adminLoginUI() {
        for (;;) {
            System.out.println("\n=== Admin Login ===");
            String username = InputHelper.readLine("Username (or 0 to cancel): ");
            if (username.equals("0")) return false;
            String password = InputHelper.readLine("Password (or 0 to cancel): ");
            if (password.equals("0")) return false;
            Admin admin = control.loginAdmin(username, password);
            if (admin != null) {
                currentAdmin = admin;
                OutputHelper.printOK("Admin login successful.");
                InputHelper.waitForEnter();
                return true;
            }
            OutputHelper.printErr("Invalid username or password.");
            System.out.println("Please try again.\n");
        }
    }
    private boolean registerUI() {
        System.out.println("\n=== Member Registration ===");
        String username;
        for (;;) {
            username = InputHelper.readLine("Username (or 0 to cancel): ");
            if (username.equals("0")) return false;
            if (username.isBlank()) {
                OutputHelper.printErr("Username cannot be empty.");
                continue;
            }
            if (control.findMemberByUsername(username) != null) {
                OutputHelper.printErr("Username already exists.");
                continue;
            }
            break;
        }
        String name = customizeNameUI();
        if (name == null) return false;
        String password = customizePasswordUI();
        if (password == null) return false;
        Member member = control.registerMember(username, password, name);
        if (member == null) {
            OutputHelper.printErr("Registration failed.");
            InputHelper.waitForEnter();
            return false;
        }
        OutputHelper.printOK("Registration successful.");
        System.out.println("Member ID: " + member.getMemberID());
        InputHelper.waitForEnter();
        return true;
    }
    private void memberMenu() {
        MenuOption[] memberOptions = {
                MenuOption.LOGOUT,
                MenuOption.PROFILE,
                MenuOption.STATUS,
                MenuOption.REDEMPTION,
                MenuOption.PROMOTION,
                MenuOption.NOTIFICATION,
                MenuOption.EARN_POINTS
        };
        for (;;) {
            if (currentMember == null) return;
            int unread = control.countUnreadNotifications(currentMember);
            String banner = "Welcome, " + currentMember.getName() + ". You have " + unread + " unread notifications.";
            MenuOption selected = Menu.prompt(TITLE, banner, memberOptions);
            switch (selected) {
                case LOGOUT:
                    currentMember = null;
                    OutputHelper.printOK("Member logged out.");
                    InputHelper.waitForEnter();
                    return;
                case PROFILE:
                    profileUI();
                    break;
                case STATUS:
                    statusUI();
                    break;
                case REDEMPTION:
                    redeemUI();
                    break;
                case PROMOTION:
                    promotionUI();
                    break;
                case NOTIFICATION:
                    notificationUI();
                    break;
                case EARN_POINTS:
                    earnPointsUI(); break;
                default:
                    break;
            }
        }
    }
    private void guestMenu() {
        MenuOption[] guestOptions = {
                MenuOption.BACK,
                MenuOption.PROFILE,
                MenuOption.PROMOTION,
                MenuOption.NOTIFICATION
        };
        for (;;) {
            MenuOption selected = Menu.prompt(TITLE, "Guest Menu", guestOptions);
            if (selected == MenuOption.BACK) return;
            switch (selected) {
                case PROFILE:
                    profileUI();
                    break;
                case PROMOTION:
                    promotionUI();
                    break;
                case NOTIFICATION:
                    notificationUI();
                    break;
                default:
                    break;
            }
        }
    }
    private void profileUI() {
        if (currentMember == null) {
            MenuOption[] loginOptions = {
                    MenuOption.BACK,
                    MenuOption.MEMBER_LOGIN
            };
            MenuOption selected = Menu.prompt(TITLE, "You're currently a guest. Please log in to view your profile.", loginOptions);
            if (selected == MenuOption.MEMBER_LOGIN) {
                if (memberLoginUI()) memberMenu();
            } else {
                OutputHelper.printErr("Failed to log in. Please try again later.");
                InputHelper.waitForEnter();
            }
            return;
        }
        MenuOption[] profileOptions = {
                MenuOption.BACK,
                MenuOption.CHANGE_NAME,
                MenuOption.CHANGE_PASSWORD
        };
        for (;;) {
            MenuOption selected = Menu.prompt(TITLE, "Manage Profile", profileOptions);
            if (selected == MenuOption.BACK) return;
            switch (selected) {
                case CHANGE_NAME:
                    String name = customizeNameUI();
                    if (name != null) {
                        control.updateMemberName(currentMember, name);
                        OutputHelper.printOK("Name updated successfully.");
                    } else {
                        OutputHelper.printErr("Action cancelled.");
                    }
                    break;
                case CHANGE_PASSWORD:
                    String password = customizePasswordUI();
                    if (password != null) {
                        control.updateMemberPassword(currentMember, password);
                        OutputHelper.printOK("Password updated successfully.");
                    } else {
                        OutputHelper.printErr("Action cancelled.");
                    }
                    break;
                default:
                    break;
            }
            InputHelper.waitForEnter();
        }
    }
    private String customizeNameUI() {
        for (;;) {
            String name = InputHelper.readLine("Name (or 0 to cancel): ");
            if (name.equals("0")) {
                return null;
            }
            if (name.isBlank()) {
                OutputHelper.printErr("Name cannot be empty.");
                continue;
            }
            return name;
        }
    }
    private String customizePasswordUI() {
        for (;;) {
            String password = InputHelper.readLine("New Password (or 0 to cancel): ");
            if (password.equals("0")) {
                return null;
            }
            if (password.isBlank()) {
                OutputHelper.printErr("Password cannot be empty.");
                continue;
            }
            String confirmPassword = InputHelper.readLine("Confirm Password: ");
            if (!password.equals(confirmPassword)) {
                OutputHelper.printErr("Passwords do not match.");
                continue;
            }
            return password;
        }
    }
    private void statusUI() {
        control.refreshMemberState(currentMember);
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Membership Status");
            System.out.println("Member ID       : " + currentMember.getMemberID());
            System.out.println("Current Tier    : " + currentMember.getCurrentTier());
            System.out.println("Current Points  : " + currentMember.getCurrentPoints());
            System.out.println();
            TierRequirement requirement = control.findTierRequirement(currentMember.getCurrentTier());
            if (requirement != null) {
                System.out.println("Upgrade Requirement: " + formatRequirement(requirement.getPointsToUpgradeTier()));
                System.out.println("Maintenance Requirement: " + formatRequirement(requirement.getPointsToDowngradeTier()));
                System.out.println("Benefits: " + requirement.getBenefits());
            }
            System.out.println();
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.REQUIREMENTS,
                    MenuOption.TIER_HISTORY
            };
            MenuOption selected = Menu.prompt(TITLE, "Membership Status", options);
            switch (selected) {
                case BACK:
                    return;
                case REQUIREMENTS:
                    displayAllTierRequirements();
                    break;
                case TIER_HISTORY:
                    tierHistoryUI();
                    break;
                default:
                    break;
            }
        }
    }
    private void displayAllTierRequirements() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("All Tier Requirements");
        LoyaltyTier[] tiers = LoyaltyTier.values();
        for (LoyaltyTier tier : tiers) {
            if (tier == LoyaltyTier.GUEST) continue;
            TierRequirement requirement = control.findTierRequirement(tier);
            if (requirement != null) {
                System.out.println("=== " + requirement.getLoyaltyTier() + " ===");
                System.out.println("Upgrade Requirement     : " + formatRequirement(requirement.getPointsToUpgradeTier()));
                System.out.println("Maintenance Requirement : " + formatRequirement(requirement.getPointsToDowngradeTier()));
                System.out.println("Benefits                : " + requirement.getBenefits());
                System.out.println();
            }
        }
        InputHelper.waitForEnter();
    }
    private void tierHistoryUI() {
        Tier[] records = control.getTierRecords(currentMember);
        if (records.length == 0) {
            OutputHelper.printBlue("No tier history available.");
            InputHelper.waitForEnter();
            return;
        }
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Tier History");
            for (int i = 0; i < records.length; i++) {
                System.out.println("[" + (i + 1) + "] " + records[i].getLoyaltyTier() + " (" + records[i].getSeason() + ")");
            }
            System.out.println();
            MenuOption[] options = { MenuOption.BACK, MenuOption.SELECT };
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            if (selected == MenuOption.BACK) return;
            int choice = InputHelper.readInt("Select Tier Record > ");
            if (choice < 1 || choice > records.length) {
                OutputHelper.printErr("Please enter number between 1 and " + records.length + ".");
                InputHelper.waitForEnter();
                continue;
            }
            displayTierDetails(control.getTierCursor(currentMember, choice - 1));
            return;
        }
    }
    private void displayTierDetails(DoublyLinkedList<Tier>.Cursor cursor) {
        Tier tier = cursor.next(); // land on the selected node; cursor now sits just after it
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Tier Record Details");
            System.out.println();
            printTier(tier);
            MenuOption[] detailOptions = { MenuOption.BACK, MenuOption.PREVIOUS, MenuOption.NEXT, MenuOption.DELETE };
            MenuOption selected = Menu.prompt(TITLE, "Tier Record Details", detailOptions);
            switch (selected) {
                case BACK:
                    return;
                case PREVIOUS:
                    cursor.previous(); // step back onto current node
                    if (cursor.hasPrevious()) {
                        tier = cursor.previous();
                        cursor.next();
                    } else {
                        cursor.next();
                        OutputHelper.printErr("This is the first tier record.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case NEXT:
                    if (cursor.hasNext()) {
                        tier = cursor.next();
                    } else {
                        OutputHelper.printErr("This is the last tier record.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case DELETE:
                    if (control.removeTierRecord(currentMember, tier)) {
                        OutputHelper.printOK("Tier record deleted.");
                        return;
                    }
                    OutputHelper.printErr("Unable to delete tier record.");
                    InputHelper.waitForEnter();
                    break;
                default:
                    break;
            }
        }
    }
    private void printTier(Tier tier) {
        System.out.println("Season          : " + tier.getSeason());
        System.out.println("Loyalty Tier    : " + tier.getLoyaltyTier());
        System.out.println("Seasonal Points : " + tier.getSeasonalPoints());
    }
    private String formatRequirement(int points) {
        if (points == -1) return "N/A";
        return points + " points";
    }
    private void redeemUI() {
        MenuOption[] options = {
                MenuOption.BACK,
                MenuOption.REWARDS,
                MenuOption.REDEMPTION_HISTORY
        };
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Redeem Rewards");
            System.out.println("Available Redeemable Points: " + currentMember.getCurrentPoints());
            System.out.println();
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            switch (selected) {
                case BACK:
                    return;
                case REWARDS:
                    availableRewardsUI();
                    break;
                case REDEMPTION_HISTORY:
                    redemptionHistoryUI();
                    break;
                default:
                    break;
            }
        }
    }
    private void availableRewardsUI() {
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Available Rewards");
            Reward[] rewards = control.getAvailableRewards();
            int availableCount = 0;
            for (Reward reward : rewards) {
                if (!reward.isAvailable()) continue;
                availableCount++;
                System.out.println("[" + reward.getRewardID() + "] " + reward.getRewardName());
                System.out.println("    Points Required: " + reward.getRequiredPoints());
                System.out.println("    " + reward.getDescription());
                System.out.println();
            }
            if (availableCount == 0) {
                System.out.println("No rewards are currently available.");
                InputHelper.waitForEnter();
                return;
            }
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.SELECT
            };
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            if (selected == MenuOption.BACK) return;
            int id = InputHelper.readInt("Select Reward ID > ");
            Reward reward = control.findRewardByID(id);
            if (reward == null || !reward.isAvailable()) {
                OutputHelper.printErr("Reward not found or is currently unavailable.");
                InputHelper.waitForEnter();
                continue;
            }
            if (currentMember.getCurrentPoints() < reward.getRequiredPoints()) {
                OutputHelper.printErr("Insufficient redeemable points.");
                InputHelper.waitForEnter();
                continue;
            }
            String confirmation = InputHelper.readLine("Redeem " + reward.getRewardName() + " for " + reward.getRequiredPoints() + " points? (Y/N): ");
            if (!confirmation.equalsIgnoreCase("Y")) {
                OutputHelper.printErr("Redemption unsuccessful.");
                continue;
            }
            if (control.redeemReward(currentMember, reward)) {
                OutputHelper.printOK("Reward redeemed successfully.");
                System.out.println("Remaining Points: " + currentMember.getCurrentPoints());
                InputHelper.waitForEnter();
            } else {
                OutputHelper.printErr("Redemption failed.");
                InputHelper.waitForEnter();
            }
        }
    }
    private void redemptionHistoryUI() {
        Redemption[] redemptions = control.getRedemptionRecords(currentMember);
        if (redemptions.length == 0) {
            OutputHelper.printBlue("No redemption history available.");
            InputHelper.waitForEnter();
            return;
        }
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Redemption History");
            for (Redemption redemption : redemptions) {
                System.out.println("[" + redemption.getRedemptionID() + "] " + redemption.getLabel());
            }
            System.out.println();
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.SELECT
            };
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            if (selected == MenuOption.BACK) return;
            int choice = InputHelper.readInt("Select Redemption > ");
            if (choice < 1 || choice > redemptions.length) {
                OutputHelper.printErr("Please enter number between 1 and " + redemptions.length + ".");
                InputHelper.waitForEnter();
                continue;
            }
            displayRedemptionDetails(control.getRedemptionCursor(currentMember, choice - 1));
            return;
        }
    }
    private void displayRedemptionDetails(DoublyLinkedList<Redemption>.Cursor cursor) {
        Redemption redemption = cursor.next();
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Redemption Record Details");
            System.out.println();
            printRedemption(redemption);
            MenuOption[] detailOptions = {
                    MenuOption.BACK,
                    MenuOption.PREVIOUS,
                    MenuOption.NEXT,
                    MenuOption.DELETE
            };
            MenuOption selected = Menu.prompt(TITLE, "Redemption Details", detailOptions);
            switch (selected) {
                case BACK:
                    return;
                case PREVIOUS:
                    cursor.previous(); // step back onto current node
                    if (cursor.hasPrevious()) {
                        redemption = cursor.previous();
                        cursor.next();
                    } else {
                        OutputHelper.printErr("This is the first redemption record.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case NEXT:
                    if (cursor.hasNext()) {
                        redemption = cursor.next();
                    } else {
                        OutputHelper.printErr("This is the last redemption record.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case DELETE:
                    if (control.removeRedemption(currentMember, redemption)) {
                        OutputHelper.printOK("Redemption record deleted.");
                        return;
                    }
                    OutputHelper.printErr("Unable to delete redemption record.");
                    InputHelper.waitForEnter();
                    break;
                default:
                    break;
            }
        }
    }
    private void printRedemption(Redemption redemption) {
        System.out.println("Label        : " + redemption.getLabel());
        System.out.println("Reward       : " + redemption.getReward());
        System.out.println("Points Spent : " + redemption.getPointsSpent());
        System.out.println("Date         : " + redemption.getRedemptionDate());
    }
    private void promotionUI() {
        Promotion[] promotions;
        if (currentMember == null) {
            promotions = control.getGuestPromotions();
        } else {
            promotions = control.getMemberPromotions(currentMember);
        }
        if (promotions.length == 0) {
            OutputHelper.printBlue("No promotions available.");
            InputHelper.waitForEnter();
            return;
        }
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Promotions");
            for (int i = 0; i < promotions.length; i++) {
                System.out.println("[" + (i + 1) + "] " + promotions[i].getLabel());
            }
            System.out.println();
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.SELECT
            };
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            if (selected == MenuOption.BACK) return;
            int choice = InputHelper.readInt("Select Promotion > ");
            if (choice < 1 || choice > promotions.length) {
                OutputHelper.printErr("Please enter number between 1 and " + promotions.length + ".");
                InputHelper.waitForEnter();
                continue;
            }
            displayPromotionDetails(control.getPromotionCursor(currentMember, choice - 1));
            return;
        }
    }
    private void displayPromotionDetails(DoublyLinkedList<Promotion>.Cursor cursor) {
        Promotion promotion = cursor.next();
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Promotion Details");
            System.out.println();
            printPromotion(promotion);
            MenuOption[] detailOptions;
            if (currentMember == null) {
                detailOptions = new MenuOption[]{
                        MenuOption.BACK,
                        MenuOption.PREVIOUS,
                        MenuOption.NEXT
                };
            } else {
                detailOptions = new MenuOption[]{
                        MenuOption.BACK,
                        MenuOption.PREVIOUS,
                        MenuOption.NEXT,
                        MenuOption.DELETE
                };
            }
            MenuOption selected = Menu.prompt(TITLE, "Promotion Details", detailOptions);
            switch (selected) {
                case BACK:
                    return;
                case PREVIOUS:
                    if (cursor.hasPrevious()) {
                        promotion = cursor.previous();
                    } else {
                        OutputHelper.printErr("This is the first promotion.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case NEXT:
                    if (cursor.hasNext()) {
                        promotion = cursor.next();
                    } else {
                        OutputHelper.printErr("This is the last promotion.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case DELETE:
                    if (control.removePromotion(currentMember, promotion)) {
                        OutputHelper.printOK("Promotion deleted.");
                        InputHelper.waitForEnter();
                        return;
                    }
                    OutputHelper.printErr("Unable to delete promotion.");
                    InputHelper.waitForEnter();
                    break;
                default:
                    break;
            }
        }
    }
    private void printPromotion(Promotion promotion) {
        System.out.println("Label       : " + promotion.getLabel());
        System.out.println("Description : " + promotion.getDescription());
        System.out.println("Start Date  : " + promotion.getStartDate());
        System.out.println("Expiry Date : " + promotion.getExpiryDate());
    }
    private void notificationUI() {
        Notification[] notifications;
        if (currentMember == null) {
            notifications = control.getGuestNotifications();
        } else {
            notifications = control.getMemberNotifications(currentMember);
        }
        if (notifications.length == 0) {
            OutputHelper.printBlue("No notifications available.");
            InputHelper.waitForEnter();
            return;
        }
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Notifications");
            for (int i = 0; i < notifications.length; i++) {
                System.out.println("[" + (i + 1) + "] " + notifications[i].getLabel() + " | " + (notifications[i].getIsRead() ? "Read" : "Unread"));
            }
            System.out.println();
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.SELECT
            };
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            if (selected == MenuOption.BACK) return;
            int choice = InputHelper.readInt("Select Notification > ");
            if (choice < 1 || choice > notifications.length) {
                OutputHelper.printErr("Please enter number between 1 and " + notifications.length + ".");
                InputHelper.waitForEnter();
                continue;
            }
            displayNotificationDetails(control.getNotificationCursor(currentMember, choice - 1));
            return;
        }
    }
    private void displayNotificationDetails(DoublyLinkedList<Notification>.Cursor cursor) {
        Notification notification = cursor.next();
        for (;;) {
            if (currentMember != null && !notification.getIsRead()) notification.read();
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Notification Details");
            System.out.println();
            printNotification(notification);
            System.out.println("Status: " + (notification.getIsRead() ? "Read" : "Unread"));
            MenuOption[] detailOptions;
            if (currentMember == null) {
                detailOptions = new MenuOption[] {
                            MenuOption.BACK,
                            MenuOption.PREVIOUS,
                            MenuOption.NEXT
                };
            } else {
               detailOptions = new MenuOption[] {
                            MenuOption.BACK,
                            MenuOption.PREVIOUS,
                            MenuOption.NEXT,
                            MenuOption.DELETE
                };
            }
            MenuOption selected = Menu.prompt(TITLE, "Notification Details", detailOptions);
            switch (selected) {
                case BACK:
                    return;
                case PREVIOUS:
                    if (cursor.hasPrevious()) {
                        notification = cursor.previous();
                    } else {
                        OutputHelper.printErr("This is the first notification.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case NEXT:
                    if (cursor.hasNext()) {
                        cursor.next();
                    } else {
                        OutputHelper.printErr("This is the last notification.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case DELETE:
                    if (control.removeNotification(currentMember, notification)) {
                        OutputHelper.printOK("Notification deleted.");
                        InputHelper.waitForEnter();
                        return;
                    }
                    OutputHelper.printErr("Unable to delete notification.");
                    InputHelper.waitForEnter();
                    break;
                default:
                    break;
            }
        }
    }
    private void printNotification(Notification notification) {
        System.out.println("Label     : " + notification.getLabel());
        System.out.println("Type      : " + notification.getType());
        System.out.println("Message   : " + notification.getMessage());
        System.out.println("Published : " + notification.getPublishedDatetime());
        System.out.println("Status    : " + (notification.getIsRead() ? "Read" : "Unread"));
    }
    private void earnPointsUI() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("Simulate Stay / Booking");
        int amount = InputHelper.readInt("Enter amount spent (RM) > ");
        if (amount <= 0) {
            OutputHelper.printErr("Amount must be positive.");
            InputHelper.waitForEnter();
            return;
        }
        if (control.earnPoints(currentMember, amount)) {
            OutputHelper.printOK("Points earned. Redeemable: " + currentMember.getCurrentPoints() + " | Seasonal: " + currentMember.getSeasonalPoints());
        } else {
            OutputHelper.printErr("Unable to award points.");
        }
        InputHelper.waitForEnter();
    }
    private void adminMenu() {
        MenuOption[] adminOptions = {
                MenuOption.LOGOUT,
                MenuOption.MANAGE_MEMBERS,
                MenuOption.MANAGE_PROMOTIONS,
                MenuOption.MANAGE_REWARDS,
                MenuOption.MANAGE_TIERS,
                MenuOption.MANAGE_NOTIFICATIONS,
                MenuOption.MANAGE_REDEMPTIONS
        };
        for (;;) {
            if (currentAdmin == null) return;
            MenuOption selected = Menu.prompt(TITLE, "Administrator: " + currentAdmin.getName(), adminOptions);
            switch (selected) {
                case LOGOUT:
                    currentAdmin = null;
                    OutputHelper.printOK("Administrator logged out.");
                    return;
                case MANAGE_MEMBERS:
                    manageMemberUI();
                    break;
                case MANAGE_PROMOTIONS:
                    managePromotionUI();
                    break;
                case MANAGE_REWARDS:
                    manageRewardUI();
                    break;
                case MANAGE_TIERS:
                    manageTierUI();
                    break;
                case MANAGE_NOTIFICATIONS:
                    manageNotificationUI();
                    break;
                case MANAGE_REDEMPTIONS:
                    manageRedemptionUI();
                    break;
                default:
                    break;
            }
            InputHelper.waitForEnter();
        }
    }
    private void manageMemberUI() {
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Manage Members");
            Member[] members = control.getAllMembers();
            for (Member member : members) {
                System.out.println("[" + member.getMemberID() + "] " + member.getName() + " | " + member.getUsername() + " | Tier: " + member.getCurrentTier() + " | Points: " + member.getCurrentPoints());
            }
            System.out.println();
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.SELECT
            };
            MenuOption selected = Menu.prompt(TITLE, "Select a member to edit", options);
            if (selected == MenuOption.BACK) return;
            Member member = promptForMember();
            if (member == null) {
                OutputHelper.printErr("Member not found.");
                InputHelper.waitForEnter();
                continue;
            }
            editMember(member);
        }
    }
    private Member promptForMember() {
        String username = InputHelper.readLine("Member Username: ");
        Member member = control.findMemberByUsername(username);
        if (member == null) {
            OutputHelper.printErr("Member not found.");
            return null;
        }
        return member;
    }
    private void editMember(Member member) {
        OutputHelper.clearScreen();
        OutputHelper.printTitle("Edit Member");
        String name = InputHelper.readLine("New Name (or 0 to cancel): ");
        if (name.equals("0")) return;
        String password = InputHelper.readLine("New Password (or 0 to cancel): ");
        if (password.equals("0")) return;
        LoyaltyTier tier = null;
        for (;;) {
            String tierInput = InputHelper.readLine("New Tier (Silver/Gold/Platinum, or 0 to cancel): ");
            if (tierInput.equals("0")) return;
            if (!tierInput.isBlank()) {
                switch (tierInput.trim().toLowerCase()) {
                    case "silver":
                        tier = LoyaltyTier.SILVER;
                        break;
                    case "gold":
                        tier = LoyaltyTier.GOLD;
                        break;
                    case "platinum":
                        tier = LoyaltyTier.PLATINUM;
                        break;
                    default:
                        OutputHelper.printErr("Invalid tier.");
                        InputHelper.waitForEnter();
                        continue;
                }
            }
            break;
        }
        int pointsInput = InputHelper.readInt("New Points (or 0 to cancel): ");
        if (pointsInput == 0) return;
        if (control.updateMember(member, name, password, tier, pointsInput)) {
            OutputHelper.printOK("Member updated successfully.");
        } else {
            OutputHelper.printErr("Unable to update member.");
        }
        InputHelper.waitForEnter();
    }
    private void managePromotionUI() {
        MenuOption[] options = {
                MenuOption.BACK,
                MenuOption.MODIFY_PROMOTION,
                MenuOption.CREATE_PROMOTION
        };
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Manage Promotion");
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            switch(selected) {
                case BACK:
                    return;
                case MODIFY_PROMOTION:
                    modifyPromotionUI();
                    break;
                case CREATE_PROMOTION:
                    createPromotionUI();
                    break;
                default:
                    break;
            }
        }
    }
    private void modifyPromotionUI() {
        Promotion[] promotions = control.getAllPromotions();
        if (promotions.length == 0) {
            OutputHelper.printBlue("No promotions available.");
            return;
        }
        for (int i = 0; i < promotions.length; i++) {
            System.out.println("[" + (i+1) + "] " + promotions[i].getLabel() + " (expires " + promotions[i].getExpiryDate() + ")");
        }
        int choice = InputHelper.readInt("Select Promotion (0 to cancel) > ");
        if (choice < 1 || choice > promotions.length) return;
        Promotion promotion = promotions[choice - 1];
        String newExpiry = InputHelper.readLine("New Expiry Date (blank to keep '" + promotion.getExpiryDate() + "'): ");
        if (!newExpiry.isBlank()) promotion.setExpiryDate(newExpiry);
        if (InputHelper.readLine("Change target tiers? (Y/N): ").equalsIgnoreCase("Y")) {
            LoyaltyTier[] oldTiers = promotion.getTargetTiers();
            LoyaltyTier[] newTiers = selectPromotionTiers();
            promotion.setTargetTiers(newTiers);
            for (Member member : control.getAllMembers()) {
                boolean was = containsTier(oldTiers, member.getCurrentTier());
                boolean now = containsTier(newTiers, member.getCurrentTier());
                if (now && !was) {
                    control.assignPromotion(member, promotion);
                    control.postPromotionNotification(member, promotion);
                } else if (!now && was) {
                    control.removePromotion(member, promotion);
                }
            }
        }
        OutputHelper.printOK("Promotion updated.");
    }
    private void createPromotionUI() {
        String label = InputHelper.readLine("Label: ");
        String description = InputHelper.readLine("Description: ");
        String startDate = InputHelper.readLine("Start Date: ");
        String expiryDate = InputHelper.readLine("Expiry Date: ");
        LoyaltyTier[] tiers = selectPromotionTiers();
        Promotion promotion = control.createPromotion(label, description, startDate, expiryDate, tiers);
        if (promotion == null) { OutputHelper.printErr("Unable to create promotion."); return; }
        for (Member member : control.getAllMembers()) {
            if (containsTier(tiers, member.getCurrentTier())) {
                control.assignPromotion(member, promotion);
                control.postPromotionNotification(member, promotion);
            }
        }
        OutputHelper.printOK("Promotion created: " + promotion.getPromotionID());
    }
    private LoyaltyTier[] selectPromotionTiers() {
        System.out.println("Select target tiers.");
        System.out.println("[1] Guest");
        System.out.println("[2] Silver");
        System.out.println("[3] Gold");
        System.out.println("[4] Platinum");
        System.out.println("Enter choices separated by commas.");
        String input = InputHelper.readLine("Tiers: ");
        String[] values = input.split(",");
        LoyaltyTier[] result = new LoyaltyTier[values.length];
        int count = 0;
        for (String value : values) {
            String trimmed = value.trim();
            switch (trimmed) {
                case "1":
                    result[count++] = LoyaltyTier.GUEST;
                    break;
                case "2":
                    result[count++] = LoyaltyTier.SILVER;
                    break;
                case "3":
                    result[count++] = LoyaltyTier.GOLD;
                    break;
                case "4":
                    result[count++] = LoyaltyTier.PLATINUM;
                    break;
                default:
                    break;
            }
        }
        if (count == result.length) return result;
        LoyaltyTier[] trimmedResult = new LoyaltyTier[count];
        System.arraycopy(result, 0, trimmedResult, 0, count);
        return trimmedResult;
    }
    private boolean containsTier(LoyaltyTier[] tiers, LoyaltyTier tier) {
        for (LoyaltyTier t : tiers) {
            if (t == tier) return true;
        }
        return false;
    }
    private void manageRewardUI() {
        MenuOption[] options = {
                MenuOption.BACK,
                MenuOption.VIEW_REWARD,
                MenuOption.ADD_REWARD
        };
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Manage Rewards");
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            switch(selected) {
                case BACK:
                    return;
                case VIEW_REWARD:
                    viewEditRewardUI();
                    break;
                case ADD_REWARD:
                    addRewardUI();
                    break;
                default:
                    break;
            }
        }
    }
    private void viewEditRewardUI() {
        for (Reward reward : control.getAvailableRewards()) {
            System.out.println("[" + reward.getRewardID() + "] " + reward.getRewardName() + " | " + reward.getRequiredPoints() + " pts | " + (reward.isAvailable() ? "Available" : "Disabled"));
        }
        int id = InputHelper.readInt("Enter Reward ID to edit/delete (0 to cancel) > ");
        if (id == 0) return;
        Reward reward = control.findRewardByID(id);
        if (reward == null) { OutputHelper.printErr("Reward not found."); return; }
        MenuOption[] options = {
                MenuOption.BACK,
                MenuOption.EDIT_REWARD,
                MenuOption.DISABLE_REWARD
        };
        MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
        switch(selected) {
            case BACK:
                return;
            case EDIT_REWARD:
                String name = InputHelper.readLine("New Name (blank to keep): ");
                if (!name.isBlank()) reward.setRewardName(name);
                String desc = InputHelper.readLine("New Description (blank to keep): ");
                if (!desc.isBlank()) reward.setDescription(desc);
                String pointsStr = InputHelper.readLine("New Required Points (blank to keep): ");
                if (!pointsStr.isBlank()) reward.setRequiredPoints(Integer.parseInt(pointsStr));
                OutputHelper.printOK("Reward updated.");
                break;
            case DISABLE_REWARD:
                if (control.disableReward(id)) {
                    OutputHelper.printOK("Reward disabled.");
                } else {
                    OutputHelper.printErr("Unable to disable reward.");
                }
        }
    }
    private void addRewardUI() {
        String name = InputHelper.readLine("Reward Name: ");
        String description = InputHelper.readLine("Description: ");
        int points = InputHelper.readInt("Required Points: ");
        OutputHelper.printOK(control.createReward(name, description, points) != null ? "Reward added." : "Unable to add reward.");
    }
    private void manageTierUI() {
        MenuOption[] options = {
                MenuOption.BACK,
                MenuOption.REQUIREMENTS,
                MenuOption.EDIT_TIER_REQUIREMENTS,
                MenuOption.END_SEASON
        };
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Manage Tier Requirements");
            System.out.println("Current Season: " + control.getSeasonSummary());
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            switch (selected) {
                case BACK:
                    return;
                case REQUIREMENTS:
                    displayAllTierRequirements();
                    break;
                case EDIT_TIER_REQUIREMENTS:
                    editTierRequirementUI();
                    break;
                case END_SEASON:
                    control.endSeason();
                    OutputHelper.printOK("Season ended. New season: " + control.getSeasonSummary());
                    break;
                default:
                    break;
            }
        }
    }
    private void editTierRequirementUI() {
        LoyaltyTier tier = selectTier();
        if (tier == LoyaltyTier.GUEST) {
            OutputHelper.printErr("Guest tier cannot be edited.");
            return;
        }
        OutputHelper.printTitle("Edit Tier Requirements");
        int upgradeRequirement = -1;
        for (;;) {
            if (tier != LoyaltyTier.PLATINUM) {
                upgradeRequirement = InputHelper.readInt("New Upgrade Requirement (or -1 to skip, 0 to cancel): ");
                if (upgradeRequirement == 0) {
                    return;
                } else if (upgradeRequirement < 0 && upgradeRequirement != -1) {
                    OutputHelper.printErr("Upgrade Requirement cannot be lower than zero.");
                    continue;
                }
            }
            break;
        }
        int maintenanceRequirement = -1;
        for (;;) {
            if (tier != LoyaltyTier.SILVER) {
                maintenanceRequirement = InputHelper.readInt("New Maintenance Requirement (or -1 to skip, 0 to cancel): ");
                if (maintenanceRequirement == 0) {
                    return;
                } else if (maintenanceRequirement < 0 && maintenanceRequirement != -1) {
                    OutputHelper.printErr("Maintenance Requirement cannot be lower than zero.");
                    continue;
                }
            }
            break;
        }
        String benefits = InputHelper.readLine("New benefits (or enter to skip): ");
        if(benefits.equals("0")) return;
        if(control.updateTierRequirement(tier, upgradeRequirement, maintenanceRequirement, benefits)) {
            OutputHelper.printOK("Tier requirement updated.");
        } else {
            OutputHelper.printErr("Unable to update tier requirements, please try again later.");
        }
    }
    private LoyaltyTier selectTier() {
        OutputHelper.printOptions(new String[]{"Back", "Guest", "Silver", "Gold", "Platinum"});
        int choice = InputHelper.readInt("Select Tier: ");
        return switch (choice) {
            case 2 -> LoyaltyTier.SILVER;
            case 3 -> LoyaltyTier.GOLD;
            case 4 -> LoyaltyTier.PLATINUM;
            default -> LoyaltyTier.GUEST;
        };
    }
    private void manageNotificationUI() {
        MenuOption[] options = {
                MenuOption.BACK,
                MenuOption.VIEW_MEMBER_NOTIFICATION,
                MenuOption.ANNOUNCE
        };
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Manage Notifications");
            MenuOption selected = Menu.prompt(TITLE, "Select an option", options);
            switch (selected) {
                case BACK:
                    return;
                case VIEW_MEMBER_NOTIFICATION:
                    adminViewMemberNotifications();
                    break;
                case ANNOUNCE:
                    makeAnnouncement();
                    break;
                default:
                    break;
            }
        }
    }
    private void adminViewMemberNotifications() {
        Member member = promptForMember();
        if (member == null) return;
        Notification[] notifications = control.getMemberNotifications(member);
        if (notifications.length == 0) {
            OutputHelper.printBlue("No notifications.");
            return;
        }
        for (int i = 0; i < notifications.length; i++) {
            System.out.println("[" + (i+1) + "] " + notifications[i].getLabel());
        }
        int choice = InputHelper.readInt("Select Notification (0 to cancel) > ");
        if (choice < 1 || choice > notifications.length) return;
        adminBrowseNotifications(control.getNotificationCursor(member, choice - 1));
    }
    private void adminBrowseNotifications(DoublyLinkedList<Notification>.Cursor cursor) {
        Notification notification = cursor.next();
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Notification Details (Admin View)");
            printNotification(notification);
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.PREVIOUS,
                    MenuOption.NEXT
            };
            MenuOption selected = Menu.prompt(TITLE, "Notification Details", options);
            switch(selected) {
                case BACK:
                    return;
                case PREVIOUS:
                    if (cursor.hasPrevious()) {
                        cursor.previous();
                    } else {
                        OutputHelper.printErr("This is the first notification.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case NEXT:
                    if (cursor.hasNext()) {
                        cursor.next();
                    } else {
                        OutputHelper.printErr("This is the last notification.");
                        InputHelper.waitForEnter();
                    }
                    break;
                default:
                    break;
            }
        }
    }
    private void makeAnnouncement() {
        String label = InputHelper.readLine("Announcement Label: ");
        String message = InputHelper.readLine("Announcement Message: ");
        control.postAnnouncement(label, message);
        OutputHelper.printOK("Announcement broadcast successfully.");
    }
    private void manageRedemptionUI() {
        Member member = promptForMember();
        if (member == null) return;
        Redemption[] redemptions = control.getRedemptionRecords(member);
        if (redemptions.length == 0) {
            OutputHelper.printBlue("No redemption records.");
            return;
        }
        for (int i = 0; i < redemptions.length; i++) {
            System.out.println("[" + (i+1) + "] " + redemptions[i].getLabel());
        }
        int choice = InputHelper.readInt("Select Redemption (0 to cancel) > ");
        if (choice < 1 || choice > redemptions.length) return;
        adminBrowseRedemptions(control.getRedemptionCursor(member, choice - 1));
    }
    private void adminBrowseRedemptions(DoublyLinkedList<Redemption>.Cursor cursor) {
        Redemption redemption = cursor.next();
        for (;;) {
            OutputHelper.clearScreen();
            OutputHelper.printTitle("Redemption Details (Admin View)");
            printRedemption(redemption);
            MenuOption[] options = {
                    MenuOption.BACK,
                    MenuOption.PREVIOUS,
                    MenuOption.NEXT
            };
            MenuOption selected = Menu.prompt(TITLE, "Redemption Details", options);
            switch(selected) {
                case BACK:
                    return;
                case PREVIOUS:
                    if (cursor.hasPrevious()) {
                        cursor.previous();
                    } else {
                        OutputHelper.printErr("This is the first redemption.");
                        InputHelper.waitForEnter();
                    }
                    break;
                case NEXT:
                    if (cursor.hasNext()) {
                        cursor.next();
                    } else {
                        OutputHelper.printErr("This is the last redemption.");
                        InputHelper.waitForEnter();
                    }
                    break;
                default:
                    break;
            }
        }
    }
}