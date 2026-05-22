package id.ac.ui.cs.advprog.mysawitbe.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record Weight(int grams) implements Comparable<Weight> {

    public Weight {
        if (grams <= 0) {
            throw new IllegalArgumentException("Weight must be positive, got: " + grams);
        }
    }

    @JsonCreator
    public static Weight of(int grams) {
        return new Weight(grams);
    }

    public Weight add(Weight other) {
        Objects.requireNonNull(other);
        return new Weight(this.grams + other.grams);
    }

    public boolean isLessThanOrEqualTo(Weight other) {
        Objects.requireNonNull(other);
        return this.grams <= other.grams;
    }

    public boolean isGreaterThan(Weight other) {
        Objects.requireNonNull(other);
        return this.grams > other.grams;
    }

    @JsonValue
    public int toGrams() {
        return grams;
    }

    @Override
    public int compareTo(Weight other) {
        Objects.requireNonNull(other);
        return Integer.compare(this.grams, other.grams);
    }

    @Override
    public String toString() {
        return grams + "g";
    }
}
