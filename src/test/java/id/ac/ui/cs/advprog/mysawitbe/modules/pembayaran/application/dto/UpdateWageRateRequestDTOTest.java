package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UpdateWageRateRequestDTOTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequest_noViolations() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("BURUH", new BigDecimal("10000"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void nullType_hasViolation() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO(null, new BigDecimal("10000"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void blankType_hasViolation() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("   ", new BigDecimal("10000"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("type")));
    }

    @Test
    void emptyType_hasViolation() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("", new BigDecimal("10000"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }

    @Test
    void nullNewRatePerGram_hasViolation() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("BURUH", null);

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newRatePerGram")));
    }

    @Test
    void zeroNewRatePerGram_hasViolation() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("BURUH", BigDecimal.ZERO);

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newRatePerGram")));
    }

    @Test
    void negativeNewRatePerGram_hasViolation() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("BURUH", new BigDecimal("-100"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("newRatePerGram")));
    }

    @Test
    void positiveNewRatePerGram_noViolations() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("SUPIR", new BigDecimal("15000"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void smallPositiveValue_noViolations() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("MANDOR", new BigDecimal("0.01"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void bothFieldsInvalid_twoViolations() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO(null, null);

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertEquals(2, violations.size());
    }

    @Test
    void typeAndRateBothValid_noViolations() {
        UpdateWageRateRequestDTO dto1 = new UpdateWageRateRequestDTO("BURUH", new BigDecimal("100"));
        UpdateWageRateRequestDTO dto2 = new UpdateWageRateRequestDTO("SUPIR", new BigDecimal("200"));
        UpdateWageRateRequestDTO dto3 = new UpdateWageRateRequestDTO("MANDOR", new BigDecimal("300"));

        assertTrue(validator.validate(dto1).isEmpty());
        assertTrue(validator.validate(dto2).isEmpty());
        assertTrue(validator.validate(dto3).isEmpty());
    }

    @Test
    void recordFieldsAccessible() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("BURUH", new BigDecimal("10000"));

        assertEquals("BURUH", dto.type());
        assertEquals(new BigDecimal("10000"), dto.newRatePerGram());
    }

    @Test
    void largePositiveRate_noViolations() {
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("ADMIN", new BigDecimal("999999999"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void typeWithWhitespace_onlyFirstValidationFails() {
        // " BURUH" is not blank, so @NotBlank passes
        // but if we want blank validation, blank means only whitespace
        UpdateWageRateRequestDTO dto = new UpdateWageRateRequestDTO("  ", new BigDecimal("100"));

        Set<ConstraintViolation<UpdateWageRateRequestDTO>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty());
    }
}