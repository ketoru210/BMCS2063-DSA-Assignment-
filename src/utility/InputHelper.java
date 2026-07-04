package utility;

import java.util.Scanner;

/**
 * Small utility class for reading and validating console input.
 * Utility classes contain only static members.
 */
public final class InputHelper {

    private static final Scanner SCANNER = new Scanner(System.in);

    private InputHelper() {
        // prevent instantiation
    }

    /** Reads a trimmed line of text from the console. */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    /** Reads an integer, re-prompting until valid input is given. */
    public static int readInt(String prompt) {
        while (true) {
            String input = readLine(prompt);
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                OutputHelper.printErr("Please enter a number.");
            }
        }
    }

    public static void waitForEnter() {
        OutputHelper.printBlue("\nPress [Enter] to continue ...");
        SCANNER.nextLine();
    }
}
