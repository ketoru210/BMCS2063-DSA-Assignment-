package entity;

/**
 * What interest a queue-jump protects, from the most to the least pressing.
 * Compared as a layer above the tier score, never added into it — addition can
 * be cancelled out by unbounded aging, layering cannot.
 * <p>
 * Constants are declared in ascending order of that interest, with gaps of 10
 * so a new category can be inserted without renumbering the rest.
 *
 * @author YZ
 */
public enum SpecialCategory {
    NONE(0, "-"),
    GOODWILL(10, "Goodwill"),
    HABITABILITY(20, "Habitability"),
    COMPLIANCE(30, "Compliance"),
    LIFE_SAFETY(40, "Life Safety");

    private final int weight;
    private final String label;

    SpecialCategory(int weight, String label) {
        this.weight = weight;
        this.label = label;
    }

    // ordering reads this, never ordinal() — inserting a constant must not shift it
    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return label;
    }
}
