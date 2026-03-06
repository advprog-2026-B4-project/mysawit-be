package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain;

/**
 * Domain model representing a single configurable wage variable.
 * Pure Java - no Spring or JPA annotations.
 * Business invariant: value must always be positive.
 */
public class VariabelPokok {

    private final VariableKey key;
    private int value;

    public VariabelPokok(VariableKey key, int value) {
        validateValue(value);
        this.key   = key;
        this.value = value;
    }

    /** Applies a new wage rate. Enforces the positive-value invariant. */
    public void updateValue(int newValue) {
        validateValue(newValue);
        this.value = newValue;
    }

    public VariableKey getKey()   { return key; }
    public int         getValue() { return value; }

    // ------------------------------------------------------------------

    private static void validateValue(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Wage rate must be a positive integer, got: " + value);
        }
    }
}
