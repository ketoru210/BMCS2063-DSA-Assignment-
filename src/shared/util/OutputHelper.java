package shared.util;

public class OutputHelper {
    private static final String resetANSI = "\033[0m";
    private static final String redANSI  = "\033[31m";
    private static final String greenANSI = "\033[32m";
    private static final String blueANSI = "\033[34m";

    public static void printTitle(String title) {
        System.out.println("=== " + title + " ===");
    }

    public static void printOK(String msg) {
        System.out.print(greenANSI);
        System.out.print(msg);
        System.out.println(resetANSI);
    }

    public static void printErr(String msg) {
        System.err.print(redANSI);
        System.err.print(msg);
        System.out.println(resetANSI);
    }

    public static void printBlue(String msg) {
        System.err.print(blueANSI);
        System.err.print(msg);
        System.out.println(resetANSI);
    }

    public static void printOptions(String[] options) {
        for (int i = 0; i < options.length; i++) {
            System.out.println("[" + i + "] " + options[i]);
        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
    }
}
