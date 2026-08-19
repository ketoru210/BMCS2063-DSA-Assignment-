package utility;

/**
 * Lays out a column table. Widths come from the content, so the divider and the
 * cells can never drift apart the way two hand-written format strings do.
 *
 * <p>Rows come back as strings rather than being printed, so the caller can
 * colour individual rows.
 *
 * @author YZ
 */
public final class TableRenderer {

    /** Which edge a column's short cells are padded against. */
    public enum Align { LEFT, RIGHT }

    private static final String GUTTER = "  ";

    private TableRenderer() {
        // prevent instantiation
    }

    /**
     * Returns a header row followed by one row per entry, columns separated by
     * two spaces and nothing drawn around them.
     *
     * @param headers one label per column
     * @param cells   one array per row, each as long as {@code headers}
     * @param aligns  one per column; {@code null} left-aligns everything
     */
    public static String[] render(String[] headers, String[][] cells, Align[] aligns) {
        int[] widths = widths(headers, cells);
        String[] rows = new String[cells.length + 1];

        rows[0] = line(headers, widths, aligns, GUTTER, "", "");
        for (int i = 0; i < cells.length; i++) {
            rows[i + 1] = line(cells[i], widths, aligns, GUTTER, "", "");
        }
        return rows;
    }

    /**
     * Same table boxed in {@code |} and {@code +---+}, with the divider above the
     * header, under it, and at the foot.
     */
    public static String[] renderBordered(String[] headers, String[][] cells, Align[] aligns) {
        int[] widths = widths(headers, cells);
        String divider = divider(widths);
        String[] rows = new String[cells.length + 4];

        rows[0] = divider;
        rows[1] = line(headers, widths, aligns, " | ", "| ", " |");
        rows[2] = divider;
        for (int i = 0; i < cells.length; i++) {
            rows[i + 3] = line(cells[i], widths, aligns, " | ", "| ", " |");
        }
        rows[rows.length - 1] = divider;
        return rows;
    }

    private static int[] widths(String[] headers, String[][] cells) {
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            widths[c] = text(headers[c]).length();
        }
        for (int r = 0; r < cells.length; r++) {
            if (cells[r] == null) {
                continue;
            }
            for (int c = 0; c < headers.length && c < cells[r].length; c++) {
                widths[c] = Math.max(widths[c], text(cells[r][c]).length());
            }
        }
        return widths;
    }

    private static String line(String[] values, int[] widths, Align[] aligns,
                               String gutter, String prefix, String suffix) {
        StringBuilder line = new StringBuilder(prefix);
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) {
                line.append(gutter);
            }
            // a caller that sized its array from a count may have left a hole
            String value = text(values != null && c < values.length ? values[c] : null);
            boolean right = aligns != null && c < aligns.length && aligns[c] == Align.RIGHT;
            line.append(pad(value, widths[c], right));
        }
        return line.append(suffix).toString();
    }

    private static String pad(String value, int width, boolean right) {
        StringBuilder padding = new StringBuilder();
        for (int i = value.length(); i < width; i++) {
            padding.append(' ');
        }
        return right ? padding + value : value + padding;
    }

    private static String divider(int[] widths) {
        StringBuilder line = new StringBuilder("+");
        for (int c = 0; c < widths.length; c++) {
            for (int i = 0; i < widths[c] + 2; i++) {
                line.append('-');
            }
            line.append('+');
        }
        return line.toString();
    }

    private static String text(String value) {
        return (value == null) ? "" : value;
    }
}
