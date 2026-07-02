package shared.util;

/**
 * Generic console-menu driver: clears the screen, prints the title and options,
 * then reads and validates the user's choice.
 *
 * <p>Convention: index {@code 0} is reserved for the exit/back option, so callers
 * loop until {@code prompt(...)} returns {@code items[0]}.
 */
public final class Menu {

    private Menu() {
        // prevent instantiation
    }

    /**
     * Displays {@code items} under {@code title} and returns the option the user selects.
     */
    public static <T extends MenuItem> T prompt(String title, T[] items) {
        return prompt(title, null, items);
    }

    /**
     * Displays {@code items} under {@code title}, with an optional {@code banner}
     * line shown between the title and the options, and returns the user's choice.
     */
    public static <T extends MenuItem> T prompt(String title, String banner, T[] items) {
        OutputHelper.clearScreen();
        OutputHelper.printTitle(title);
        System.out.println();
        if (banner != null) {
            System.out.println(banner + "\n");
        }

        String[] labels = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            labels[i] = items[i].label();
        }
        OutputHelper.printOptions(labels);

        for (;;) {
            int choice = InputHelper.readInt("\nPlease Select > ");
            if (choice >= 0 && choice < items.length) {
                return items[choice];
            }
            OutputHelper.printErr("Please enter number between 0 and " + (items.length - 1) + " (inclusive)");
        }
    }
}
