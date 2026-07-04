package utility;

/**
 * A single selectable entry in a console menu.
 * Implemented by each module's menu enum so {@link Menu} can drive it generically.
 */
public interface MenuItem {
    /** Text shown next to the option index. */
    String label();

    /** Action performed when this option is selected. */
    void run();
}
