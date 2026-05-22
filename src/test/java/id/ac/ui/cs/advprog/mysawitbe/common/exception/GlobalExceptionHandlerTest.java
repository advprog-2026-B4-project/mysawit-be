package id.ac.ui.cs.advprog.mysawitbe.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.common.dto.ValidationErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // =========================================================================
    // EntityNotFoundException → 404
    // =========================================================================
    @Nested
    class HandleEntityNotFound {

        @Test
        void handleEntityNotFound_validException_returns404() {
            EntityNotFoundException ex = new EntityNotFoundException("User not found");

            ResponseEntity<ApiResponse<Void>> response = handler.handleEntityNotFound(ex);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        void handleEntityNotFound_validException_returnsErrorResponseWithMessage() {
            String message = "Panen dengan ID abc tidak ditemukan";
            EntityNotFoundException ex = new EntityNotFoundException(message);

            ResponseEntity<ApiResponse<Void>> response = handler.handleEntityNotFound(ex);

            assertNotNull(response.getBody());
            assertThat(response.getBody().success()).isFalse();
            assertEquals(message, response.getBody().message());
        }

        @Test
        void handleEntityNotFound_validException_responseHasTimestamp() {
            EntityNotFoundException ex = new EntityNotFoundException("Not found");

            ResponseEntity<ApiResponse<Void>> response = handler.handleEntityNotFound(ex);

            assertNotNull(response.getBody().timestamp());
        }
    }

    // =========================================================================
    // MethodArgumentNotValidException → 400
    // =========================================================================
    @Nested
    class HandleValidation {

        @Test
        void handleValidation_singleFieldError_returns400WithFieldErrors() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "berat", "Berat tidak boleh kosong"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<ValidationErrorResponse>> response =
                    handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertThat(response.getBody().success()).isFalse();
            assertEquals("Validation failed", response.getBody().message());
            assertThat(response.getBody().data().fieldErrors()).containsKey("berat");
            assertEquals("Berat tidak boleh kosong",
                    response.getBody().data().fieldErrors().get("berat"));
        }

        @Test
        void handleValidation_multipleFieldErrors_returns400WithAllErrors() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "berat", "Berat harus diisi"));
            bindingResult.addError(new FieldError("request", "deskripsi", "Deskripsi tidak boleh kosong"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<ValidationErrorResponse>> response =
                    handler.handleValidation(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertThat(response.getBody().data().fieldErrors()).hasSize(2);
            assertThat(response.getBody().data().fieldErrors()).containsKeys("berat", "deskripsi");
        }

        @Test
        void handleValidation_fieldErrorWithNullDefaultMessage_usesFallback() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "berat", null));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<ValidationErrorResponse>> response =
                    handler.handleValidation(ex);

            assertEquals("Invalid value",
                    response.getBody().data().fieldErrors().get("berat"));
        }

        @Test
        void handleValidation_duplicateFieldName_keepsFirstError() throws Exception {
            BeanPropertyBindingResult bindingResult =
                    new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "berat", "Error pertama"));
            bindingResult.addError(new FieldError("request", "berat", "Error kedua"));
            MethodArgumentNotValidException ex =
                    new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<ValidationErrorResponse>> response =
                    handler.handleValidation(ex);

            assertEquals("Error pertama",
                    response.getBody().data().fieldErrors().get("berat"));
        }
    }

    // =========================================================================
    // IllegalArgumentException → 400
    // =========================================================================
    @Nested
    class HandleIllegalArgument {

        @Test
        void handleIllegalArgument_validException_returns400() {
            IllegalArgumentException ex = new IllegalArgumentException("Invalid parameter");

            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        void handleIllegalArgument_validException_returnsErrorWithMessage() {
            String message = "Hanya file JPG dan PNG yang diizinkan.";
            IllegalArgumentException ex = new IllegalArgumentException(message);

            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

            assertThat(response.getBody().success()).isFalse();
            assertEquals(message, response.getBody().message());
        }
    }

    // =========================================================================
    // IllegalStateException → 409
    // =========================================================================
    @Nested
    class HandleIllegalState {

        @Test
        void handleIllegalState_validException_returns409() {
            IllegalStateException ex = new IllegalStateException("Conflict state");

            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        void handleIllegalState_validException_returnsErrorWithMessage() {
            String message = "Pencatatan gagal: Batas harian tercapai";
            IllegalStateException ex = new IllegalStateException(message);

            ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalState(ex);

            assertThat(response.getBody().success()).isFalse();
            assertEquals(message, response.getBody().message());
        }
    }

    // =========================================================================
    // ObjectOptimisticLockingFailureException → 409
    // =========================================================================
    @Nested
    class HandleOptimisticLock {

        @Test
        void handleOptimisticLock_validException_returns409() {
            ObjectOptimisticLockingFailureException ex =
                    new ObjectOptimisticLockingFailureException("Wallet", "id");

            ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLock(ex);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        }

        @Test
        void handleOptimisticLock_validException_returnsRetryMessage() {
            ObjectOptimisticLockingFailureException ex =
                    new ObjectOptimisticLockingFailureException("Wallet", new RuntimeException());

            ResponseEntity<ApiResponse<Void>> response = handler.handleOptimisticLock(ex);

            assertThat(response.getBody().success()).isFalse();
            assertEquals("Wallet was modified concurrently. Please retry the operation.",
                    response.getBody().message());
        }
    }

    // =========================================================================
    // ArithmeticException / NumberFormatException → 422
    // =========================================================================
    @Nested
    class HandleAmountParseError {

        @Test
        void handleAmountParseError_arithmeticException_returns422() {
            ArithmeticException ex = new ArithmeticException("/ by zero");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAmountParseError(ex);

            assertEquals(422, response.getStatusCode().value());
        }

        @Test
        void handleAmountParseError_numberFormatException_returns422() {
            NumberFormatException ex = new NumberFormatException("For input string: \"abc\"");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAmountParseError(ex);

            assertEquals(422, response.getStatusCode().value());
        }

        @Test
        void handleAmountParseError_validException_includesMessagePrefix() {
            NumberFormatException ex = new NumberFormatException("For input string: \"abc\"");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAmountParseError(ex);

            assertTrue(response.getBody().message().startsWith("Invalid payment amount:"));
            assertTrue(response.getBody().message().contains("abc"));
        }
    }

    // =========================================================================
    // Generic Exception → 500
    // =========================================================================
    @Nested
    class HandleGeneric {

        @Test
        void handleGeneric_runtimeException_returns500() {
            RuntimeException ex = new RuntimeException("Something went wrong");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        void handleGeneric_generalException_returns500() {
            Exception ex = new Exception("General failure");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        void handleGeneric_validException_includesMessage() {
            Exception ex = new Exception("Database connection lost");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

            assertThat(response.getBody().success()).isFalse();
            assertEquals("An unexpected error occurred: Database connection lost",
                    response.getBody().message());
        }

        @Test
        void handleGeneric_nullMessageException_handlesGracefully() {
            Exception ex = new Exception();

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneric(ex);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertThat(response.getBody().message()).contains("An unexpected error occurred");
        }
    }

    // Mock object for MethodArgumentNotValidException tests
    private static class Object {}

    // Helper to create MethodArgumentNotValidException in a test-friendly way
    @SuppressWarnings("unused")
    private static class TestRequest {
        private String berat;
        private String deskripsi;
    }
}
