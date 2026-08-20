package utility;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Lam Yong Zhe
 */
public class OutputHelper {
    private static final String resetANSI = "\033[0m" ;
    private static final String redANSI   = "\033[31m";
    private static final String greenANSI = "\033[32m";
    private static final String blueANSI  = "\033[34m";

    private static final int REPORT_WIDTH = 66;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public static void printTitle(String title) {
        System.out.println("=== " + title + " ===");
    }

    /**
     * Report banner. Criteria is echoed because a report read on paper has to
     * say what it was run against; pass null when the report takes no input.
     */
    public static void printReportHeader(String reportName, String criteria) {
        String rule = "=".repeat(REPORT_WIDTH);
        System.out.println(rule);
        System.out.println(centre("TARUMT RESORTS"));
        System.out.println(centre(reportName));
        System.out.println(rule);
        System.out.println("  Generated : " + LocalDateTime.now().format(STAMP));
        if (criteria != null && !criteria.isBlank()) {
            System.out.println("  Criteria  : " + criteria);
        }
        System.out.println(rule);
    }

    public static void printReportFooter(String summary) {
        System.out.println("-".repeat(REPORT_WIDTH));
        if (summary != null && !summary.isBlank()) {
            System.out.println("  " + summary);
        }
        System.out.println(centre("*** End of Report ***"));
    }

    private static String centre(String text) {
        int pad = (REPORT_WIDTH - text.length()) / 2;
        return pad <= 0 ? text : " ".repeat(pad) + text;
    }

    public static void printOK(String msg) {
        System.out.println(greenANSI + msg + resetANSI);
    }

    public static void printErr(String msg) {
        System.out.println(redANSI + msg + resetANSI);
    }

    public static void printBlue(String msg) {
        System.out.println(blueANSI + msg + resetANSI);
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
