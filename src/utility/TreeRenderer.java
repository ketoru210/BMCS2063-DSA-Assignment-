package utility;

/**
 * Draws a binary tree held in level order — index 0 is the root, the children of
 * {@code i} sit at {@code 2i+1} and {@code 2i+2}, and a {@code null} slot is a
 * node that is not there.
 *
 * <p>Rows come back as strings rather than being printed, so the caller decides
 * what to colour: a heap wants its root highlighted, a search tree does not.
 *
 * <p>Width grows as {@code 2^layers}, so a tree deep enough to overflow the
 * console has to be handed over one layer-capped slice at a time.
 *
 * @author Lam Yong Zhe
 */
public final class TreeRenderer {

    private TreeRenderer() {
        // prevent instantiation
    }

    /**
     * Lays every node on the slot its index owns, so a complete tree comes out
     * symmetrical — the shape a heap is meant to show. Width doubles per layer,
     * so a sparse tree wastes most of it: use {@link #renderCompact} for those.
     *
     * <p>Returns the drawing one line per element: node row, branch row, node
     * row, and so on, so {@code 2 * layers - 1} rows in all.
     *
     * @param slots   level-order slots; {@code null} marks a missing node
     * @param layers  how many layers to draw
     * @param minCell narrowest a node may sit in, before its label is measured
     */
    public static String[] renderComplete(String[] slots, int layers, int minCell) {
        if (layers <= 0 || slots.length == 0) {
            return new String[0];
        }

        // every row's geometry is derived from the cell, so the widest label has
        // to be known before the first row can be placed
        int cell = minCell;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                cell = Math.max(cell, slots[i].length() + 2);
            }
        }

        String[][] labels = new String[layers][];
        int[][] centres = new int[layers][];

        for (int depth = 0; depth < layers; depth++) {
            int first = (1 << depth) - 1;
            int count = Math.max(0, Math.min(1 << depth, slots.length - first));
            int block = cell * (1 << (layers - 1 - depth));

            labels[depth] = new String[count];
            centres[depth] = new int[count];
            for (int k = 0; k < count; k++) {
                String label = slots[first + k];
                // a missing node still owns its slot, or the shape would collapse
                int width = (label == null) ? 1 : label.length();
                int column = k * block + (block - width) / 2;
                labels[depth][k] = label;
                centres[depth][k] = column + (width - 1) / 2;
            }
        }

        String[] rows = new String[layers * 2 - 1];
        int r = 0;
        for (int depth = 0; depth < layers; depth++) {
            rows[r++] = nodeRow(labels[depth], centres[depth]);
            if (depth + 1 < layers) {
                rows[r++] = branchRow(labels[depth + 1], centres[depth + 1]);
            }
        }
        return rows;
    }

    private static String nodeRow(String[] labels, int[] centres) {
        StringBuilder line = new StringBuilder();
        for (int k = 0; k < labels.length; k++) {
            if (labels[k] == null) {
                continue;
            }
            int start = centres[k] - (labels[k].length() - 1) / 2;
            while (line.length() < start) {
                line.append(' ');
            }
            line.append(labels[k]);
        }
        return line.toString();
    }

    /**
     * Brackets each sibling pair. A single slash cannot span the gap — the
     * horizontal distance to a child is a quarter of the parent's block, far
     * more than one column — so the dashes carry the distance and the slashes
     * only mark which end is whose. A pair with one child drops the dashes:
     * there is nothing on the other end to reach.
     */
    private static String branchRow(String[] childLabels, int[] childCentres) {
        StringBuilder line = new StringBuilder();
        for (int k = 0; k < childCentres.length; k += 2) {
            boolean left = childLabels[k] != null;
            boolean right = k + 1 < childCentres.length && childLabels[k + 1] != null;

            if (left) {
                while (line.length() < childCentres[k]) {
                    line.append(' ');
                }
                line.append('/');
            }
            if (right) {
                while (line.length() < childCentres[k + 1]) {
                    line.append(left ? '-' : ' ');
                }
                line.append('\\');
            }
        }
        return line.toString();
    }

    /**
     * Lays nodes out by in-order position instead of by slot, which puts every
     * parent between its own two subtrees and makes the width grow with the
     * number of nodes rather than with the height. A search tree that leans one
     * way — the usual case — draws narrow and stays centred on what is actually
     * there.
     *
     * <p>Two nodes on one row are always at least two apart in in-order, so half
     * a label's width per step is enough to keep them from touching.
     *
     * @param slots level-order slots; {@code null} marks a missing node
     * @param gap   blank columns to keep between neighbours on the same row
     */
    public static String[] renderCompact(String[] slots, int gap) {
        int longest = 0;
        int depth = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                longest = Math.max(longest, slots[i].length());
                depth = Math.max(depth, depthOf(i));
            }
        }
        if (depth < 0) {
            return new String[0];
        }

        int step = (longest + gap + 1) / 2;
        int[] centres = new int[slots.length];
        inOrder(slots, 0, centres, new int[]{0}, step);
        centreOverChildren(slots, centres, depth, longest + gap);

        int leftmost = Integer.MAX_VALUE;
        int width = 0;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                int start = centres[i] - (slots[i].length() - 1) / 2;
                leftmost = Math.min(leftmost, start);
                width = Math.max(width, start + slots[i].length());
            }
        }
        // the leftmost label reaches further left than its own centre
        int shift = (leftmost < 0) ? -leftmost : 0;
        width += shift;

        String[] rows = new String[depth * 2 + 1];
        for (int d = 0; d <= depth; d++) {
            rows[d * 2] = nodeLine(slots, centres, shift, width, d);
            if (d < depth) {
                rows[d * 2 + 1] = branchLine(slots, centres, shift, width, d);
            }
        }
        return rows;
    }

    /**
     * In-order rank alone leaves a parent wherever its own key falls, which drags
     * the root of a one-sided tree out to the edge. Walking back up and sitting
     * each parent at the midpoint of its children puts it over what it owns; the
     * running left edge is what stops a row from overlapping itself.
     */
    private static void centreOverChildren(String[] slots, int[] centres, int depth, int apart) {
        for (int d = depth - 1; d >= 0; d--) {
            int leftEdge = Integer.MIN_VALUE;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == null || depthOf(i) != d) {
                    continue;
                }
                int left = 2 * i + 1;
                int right = 2 * i + 2;
                boolean hasLeft = left < slots.length && slots[left] != null;
                boolean hasRight = right < slots.length && slots[right] != null;

                if (hasLeft && hasRight) {
                    centres[i] = (centres[left] + centres[right]) / 2;
                } else if (hasLeft) {
                    centres[i] = centres[left];
                } else if (hasRight) {
                    centres[i] = centres[right];
                }
                if (leftEdge != Integer.MIN_VALUE && centres[i] < leftEdge) {
                    centres[i] = leftEdge;
                }
                leftEdge = centres[i] + apart;
            }
        }
    }

    private static void inOrder(String[] slots, int index, int[] centres, int[] rank, int step) {
        if (index >= slots.length || slots[index] == null) {
            return;
        }
        inOrder(slots, 2 * index + 1, centres, rank, step);
        centres[index] = rank[0]++ * step;
        inOrder(slots, 2 * index + 2, centres, rank, step);
    }

    private static int depthOf(int index) {
        int depth = 0;
        while (index > 0) {
            index = (index - 1) / 2;
            depth++;
        }
        return depth;
    }

    private static String nodeLine(String[] slots, int[] centres, int shift, int width, int depth) {
        char[] line = blank(width);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && depthOf(i) == depth) {
                String label = slots[i];
                int start = centres[i] + shift - (label.length() - 1) / 2;
                for (int k = 0; k < label.length(); k++) {
                    line[start + k] = label.charAt(k);
                }
            }
        }
        return trim(line);
    }

    private static String branchLine(String[] slots, int[] centres, int shift, int width, int depth) {
        char[] line = blank(width);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null || depthOf(i) != depth) {
                continue;
            }
            int parent = centres[i] + shift;
            reach(line, slots, centres, shift, 2 * i + 1, parent, '/');
            reach(line, slots, centres, shift, 2 * i + 2, parent, '\\');
        }
        return trim(line);
    }

    /** Runs dashes from the parent's column out to the child's, then marks the end. */
    private static void reach(char[] line, String[] slots, int[] centres, int shift,
                              int child, int parent, char mark) {
        if (child >= slots.length || slots[child] == null) {
            return;
        }
        int end = centres[child] + shift;
        for (int c = Math.min(parent, end); c <= Math.max(parent, end); c++) {
            line[c] = '-';
        }
        line[end] = mark;
    }

    private static char[] blank(int width) {
        char[] line = new char[width];
        for (int i = 0; i < width; i++) {
            line[i] = ' ';
        }
        return line;
    }

    private static String trim(char[] line) {
        int end = line.length;
        while (end > 0 && line[end - 1] == ' ') {
            end--;
        }
        return new String(line, 0, end);
    }
}
