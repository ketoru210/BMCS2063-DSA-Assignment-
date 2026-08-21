package utility;

/**
 * Vertical bar charts for console reports, shared by M2 and M4 so the two
 * modules' reports do not each grow their own chart code and drift apart.
 *
 * <p>Rows come back as strings rather than being printed, matching
 * {@link TableRenderer}, so the caller can indent or colour them.
 *
 * <p>The glyphs are gathered in one place on purpose: a console whose JVM
 * cannot encode them (Windows defaults {@code stdout.encoding} to Cp1252)
 * turns them into question marks, and swapping the four constants below for
 * ASCII is then the whole fix.
 *
 * @author Lam Yong Zhe
 */
public final class ChartRenderer {

    private static final String[] FILLS = {"█", "▒"};
    private static final String CORNER = "└";
    private static final String RULE = "─";
    private static final String WALL = "│";
    private static final String UP = "↑";
    private static final String RIGHT = "→";

    private static final int SINGLE_BAR = 5;
    private static final int GROUPED_BAR = 3;
    private static final int GAP = 2;

    private ChartRenderer() {
        // prevent instantiation
    }

    /** One bar per label, scaled so the tallest fills {@code height} rows. */
    public static String[] bars(String[] labels, int[] values,
            String valueTitle, String axisTitle, int height) {
        return plot(labels, new int[][]{values}, null, valueTitle, axisTitle, height, SINGLE_BAR);
    }

    /**
     * Two bars per label, for the questions that are a comparison rather than a
     * distribution - wanted against available, expected against ready.
     */
    public static String[] groupedBars(String[] labels, int[] first, int[] second,
            String[] seriesNames, String valueTitle, String axisTitle, int height) {
        return plot(labels, new int[][]{first, second}, seriesNames,
                valueTitle, axisTitle, height, GROUPED_BAR);
    }

    private static String[] plot(String[] labels, int[][] series, String[] seriesNames,
            String valueTitle, String axisTitle, int height, int barWidth) {
        int max = 0;
        for (int[] values : series) {
            for (int value : values) {
                max = Math.max(max, value);
            }
        }
        if (max <= 0) {
            return new String[]{"(nothing to plot)"};
        }

        // one row per unit while the numbers are small, so the axis reads 1..n
        int rows = Math.min(height, max);
        int groupWidth = series.length * barWidth + (series.length - 1);
        int[] columns = new int[labels.length];
        for (int i = 0; i < labels.length; i++) {
            columns[i] = Math.max(groupWidth, labels[i].length());
        }

        int axisWidth = String.valueOf(scaleAt(rows, rows, max)).length();
        String indent = " ".repeat(axisWidth);
        int plotWidth = 0;
        for (int column : columns) {
            plotWidth += GAP + column;
        }

        String[] out = new String[rows + 4 + (seriesNames == null ? 0 : 1)];
        int at = 0;
        // the vertical axis needs saying in words too, or the numbers up the side
        // are a scale for nothing
        out[at++] = indent + " " + valueTitle;
        out[at++] = indent + " " + UP;

        for (int row = rows; row >= 1; row--) {
            StringBuilder line = new StringBuilder();
            line.append(String.format("%" + axisWidth + "d", scaleAt(row, rows, max)));
            line.append(" ").append(WALL);
            for (int i = 0; i < labels.length; i++) {
                StringBuilder group = new StringBuilder();
                for (int s = 0; s < series.length; s++) {
                    if (s > 0) {
                        group.append(" ");
                    }
                    boolean filled = (long) series[s][i] * rows >= (long) row * max;
                    group.append((filled ? FILLS[s % FILLS.length] : " ").repeat(barWidth));
                }
                line.append(" ".repeat(GAP)).append(centre(group.toString(), columns[i]));
            }
            out[at++] = trimRight(line.toString());
        }

        out[at++] = indent + " " + CORNER + RULE.repeat(plotWidth) + RIGHT + " " + axisTitle;

        StringBuilder names = new StringBuilder(indent + "  ");
        for (int i = 0; i < labels.length; i++) {
            names.append(" ".repeat(GAP)).append(centre(labels[i], columns[i]));
        }
        out[at++] = trimRight(names.toString());

        if (seriesNames != null) {
            StringBuilder legend = new StringBuilder(indent + "  ");
            for (int s = 0; s < seriesNames.length; s++) {
                if (s > 0) {
                    legend.append("   ");
                }
                legend.append(FILLS[s % FILLS.length]).append(" ").append(seriesNames[s]);
            }
            out[at] = legend.toString();
        }
        return out;
    }

    /** The value the given row stands for once the tallest bar is scaled to fit. */
    private static int scaleAt(int row, int rows, int max) {
        return (int) Math.round((double) row * max / rows);
    }

    private static String centre(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int left = (width - text.length()) / 2;
        return " ".repeat(left) + text + " ".repeat(width - text.length() - left);
    }

    private static String trimRight(String line) {
        int end = line.length();
        while (end > 0 && line.charAt(end - 1) == ' ') {
            end--;
        }
        return line.substring(0, end);
    }
}
