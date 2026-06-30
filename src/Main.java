import shared.util.InputHelper;
import shared.util.OutputHelper;

public class Main {
    private static final String title = "TARUMT Resorts";

    private enum MenuOption {
        EXIT("Exit",
                () -> {}
        ),
        WALK_IN("Walk-In Registration & Standard Booking Procedure",
                () -> System.out.println("//TODO: Redirect to Module 1")
        ),
        VIP_PRIORITY("VIP & Loyalty Tier-Priority Room Allocation",
                () -> System.out.println("//TODO: Redirect to Module 2")
        ),
        HOUSEKEEPING("Housekeeping and Task Log",
                () -> System.out.println("//TODO: Redirect to Module 3")
        ),
        FRONT_DESK("Front-Desk Service",
                () -> System.out.println("//TODO: Redirect to Module 4")
        ),
        LOYALTY("Loyalty and Rewards Service",
                () -> System.out.println("//TODO: Redirect to Module 5")
        );

        private final String label;
        private final Runnable action;

        MenuOption(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        void run() {
            action.run();
        }

        static String[] labels() {
            MenuOption[] values = values();
            String[] labels = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                labels[i] = values[i].label;
            }
            return labels;
        }
    }

    public static void main(String[] args) {
        for (;;) {
            displayMenu();
            MenuOption selected = getOption("\nPlease Select > ");

            if (selected == MenuOption.EXIT) {
                return;
            }
            selected.run();
            InputHelper.waitForEnter();
        }
    }

    private static void displayMenu() {
        OutputHelper.clearScreen();
        OutputHelper.printTitle(title);
        System.out.println("(^_^)/ Welcome!\n");
        OutputHelper.printOptions(MenuOption.labels());
    }

    private static MenuOption getOption(String prompt) {
        MenuOption[] values = MenuOption.values();
        for (;;) {
            int choice = InputHelper.readInt(prompt);

            if (choice < 0 || choice >= values.length) {
                OutputHelper.printErr("Please enter number between 0 and " + (values.length - 1) + " (inclusive)");
                continue;
            }
            return values[choice];
        }
    }
}
