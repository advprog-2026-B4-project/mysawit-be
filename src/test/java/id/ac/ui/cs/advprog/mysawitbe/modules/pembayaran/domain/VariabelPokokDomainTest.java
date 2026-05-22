package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class VariabelPokokDomainTest {

    @Test
    void constructor_withPositiveValue_succeeds() {
        VariabelPokok vp = new VariabelPokok(VariableKey.UPAH_BURUH, 10_000);
        assertThat(vp.getKey()).isEqualTo(VariableKey.UPAH_BURUH);
        assertThat(vp.getValue()).isEqualTo(10_000);
    }

    @Test
    void constructor_withZeroValue_throws() {
        assertThatThrownBy(() -> new VariabelPokok(VariableKey.UPAH_SUPIR, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_withNegativeValue_throws() {
        assertThatThrownBy(() -> new VariabelPokok(VariableKey.UPAH_MANDOR, -500))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateValue_withPositiveValue_updatesSuccessfully() {
        VariabelPokok vp = new VariabelPokok(VariableKey.UPAH_BURUH, 10_000);
        vp.updateValue(15_000);
        assertThat(vp.getValue()).isEqualTo(15_000);
    }

    @Test
    void updateValue_withZero_throws() {
        VariabelPokok vp = new VariabelPokok(VariableKey.UPAH_BURUH, 10_000);
        assertThatThrownBy(() -> vp.updateValue(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void variableKey_hasCorrectLabels() {
        assertThat(VariableKey.UPAH_BURUH.getLabel()).isEqualTo("Upah Buruh per Kg");
        assertThat(VariableKey.UPAH_SUPIR.getLabel()).isEqualTo("Upah Supir Truk per Kg");
        assertThat(VariableKey.UPAH_MANDOR.getLabel()).isEqualTo("Upah Mandor per Kg");
    }
}
