package id.ac.ui.cs.advprog.mysawitbe.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

/**
 * Immutable value object representing a monetary amount in smallest currency unit (e.g., IDR cents).
 * <p>
 * Design invariants:
 * <ul>
 *   <li>The internal representation is {@code long} (smallest unit) — no floating-point loss.</li>
 *   <li>All arithmetic returns a new instance (immutable).</li>
 *   <li>Serialized as a plain long via {@code @JsonValue} / {@code @JsonCreator}.</li>
 * </ul>
 */
public record Money(long amountSmallestUnit) implements Comparable<Money> {

    // ── static factories ────────────────────────────────────────────────

    /** The zero amount. */
    public static final Money ZERO = new Money(0);

    /**
     * Convenience factory: same as {@code new Money(amountSmallestUnit)}.
     */
    @JsonCreator
    public static Money of(long amountSmallestUnit) {
        return new Money(amountSmallestUnit);
    }

    // ── arithmetic ──────────────────────────────────────────────────────

    /** Returns a new Money representing {@code this + other}. */
    public Money add(Money other) {
        Objects.requireNonNull(other);
        return new Money(this.amountSmallestUnit + other.amountSmallestUnit);
    }

    /** Returns a new Money representing {@code this + amount}. */
    public Money add(long amountSmallestUnit) {
        return new Money(this.amountSmallestUnit + amountSmallestUnit);
    }

    /** Returns a new Money representing {@code this - other}. */
    public Money subtract(Money other) {
        Objects.requireNonNull(other);
        return new Money(this.amountSmallestUnit - other.amountSmallestUnit);
    }

    /** Returns a new Money representing {@code this - amount}. */
    public Money subtract(long amountSmallestUnit) {
        return new Money(this.amountSmallestUnit - amountSmallestUnit);
    }

    /** Returns a new Money whose amount is multiplied by {@code multiplier}. */
    public Money multiply(long multiplier) {
        return new Money(this.amountSmallestUnit * multiplier);
    }

    // ── predicates ──────────────────────────────────────────────────────

    /** {@code true} iff the stored amount is &gt; 0. */
    public boolean isPositive() {
        return amountSmallestUnit > 0;
    }

    /** {@code true} iff the stored amount is ≤ 0. */
    public boolean isNotPositive() {
        return amountSmallestUnit <= 0;
    }

    /** {@code true} iff this amount is ≥ {@code other}. */
    public boolean isGreaterThanOrEqualTo(Money other) {
        Objects.requireNonNull(other);
        return this.amountSmallestUnit >= other.amountSmallestUnit;
    }

    /** {@code true} iff this amount is &lt; {@code other}. */
    public boolean isLessThan(Money other) {
        Objects.requireNonNull(other);
        return this.amountSmallestUnit < other.amountSmallestUnit;
    }

    // ── JSON / toString ─────────────────────────────────────────────────

    @JsonValue
    public long toSmallestUnit() {
        return amountSmallestUnit;
    }

    @Override
    public String toString() {
        return String.valueOf(amountSmallestUnit);
    }

    // ── Comparable ──────────────────────────────────────────────────────

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other);
        return Long.compare(this.amountSmallestUnit, other.amountSmallestUnit);
    }
}
