package id.ac.ui.cs.advprog.mysawitbe.common.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    // =========================================================================
    // success(data)
    // =========================================================================
    @Nested
    class SuccessWithData {

        @Test
        void success_dataOnly_returnsSuccessResponse() {
            String data = "hello world";

            ApiResponse<String> response = ApiResponse.success(data);

            assertTrue(response.success());
            assertEquals("Operation successful", response.message());
            assertEquals(data, response.data());
            assertNull(response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        void success_dataOnly_nullData_returnsNullData() {
            ApiResponse<Void> response = ApiResponse.success(null);

            assertTrue(response.success());
            assertNull(response.data());
        }

        @Test
        void success_dataOnly_complexObject_returnsSameObject() {
            var data = Map.of("key", "value", "count", 42);

            ApiResponse<Map<String, ?>> response = ApiResponse.success(data);

            assertEquals(data, response.data());
        }
    }

    // =========================================================================
    // success(message, data)
    // =========================================================================
    @Nested
    class SuccessWithMessage {

        @Test
        void success_messageAndData_returnsSuccessResponse() {
            String message = "User created successfully";
            String data = "userId-123";

            ApiResponse<String> response = ApiResponse.success(message, data);

            assertTrue(response.success());
            assertEquals(message, response.message());
            assertEquals(data, response.data());
            assertNull(response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        void success_messageAndData_customMessageOverridesDefault() {
            String message = "Custom success message";

            ApiResponse<Void> response = ApiResponse.success(message, null);

            assertEquals(message, response.message());
        }
    }

    // =========================================================================
    // error(message)
    // =========================================================================
    @Nested
    class ErrorWithMessage {

        @Test
        void error_messageOnly_returnsErrorResponse() {
            String message = "User not found";

            ApiResponse<Void> response = ApiResponse.error(message);

            assertThat(response.success()).isFalse();
            assertEquals(message, response.message());
            assertNull(response.data());
            assertNull(response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        void error_messageOnly_blankMessage_returnsErrorResponse() {
            ApiResponse<Void> response = ApiResponse.error("");

            assertThat(response.success()).isFalse();
            assertEquals("", response.message());
        }
    }

    // =========================================================================
    // error(message, errorObject)
    // =========================================================================
    @Nested
    class ErrorWithMessageAndDetails {

        @Test
        void error_messageAndErrorObject_returnsErrorResponse() {
            String message = "Validation failed";
            Map<String, String> errorDetails = Map.of("field", "is required");

            ApiResponse<Map<String, String>> response = ApiResponse.error(message, errorDetails);

            assertThat(response.success()).isFalse();
            assertEquals(message, response.message());
            assertNull(response.data());
            assertEquals(errorDetails, response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        void error_messageAndErrorObject_nullErrorObject_returnsNullError() {
            ApiResponse<Void> response = ApiResponse.error("Error", null);

            assertNull(response.error());
        }
    }

    // =========================================================================
    // fail(message, data)
    // =========================================================================
    @Nested
    class Fail {

        @Test
        void fail_messageAndData_returnsFailedResponse() {
            String message = "Validation failed";
            ValidationErrorResponse validationData = new ValidationErrorResponse(
                    Map.of("email", "Invalid format"));

            ApiResponse<ValidationErrorResponse> response = ApiResponse.fail(message, validationData);

            assertThat(response.success()).isFalse();
            assertEquals(message, response.message());
            assertEquals(validationData, response.data());
            assertNull(response.error());
            assertNotNull(response.timestamp());
        }

        @Test
        void fail_messageAndData_nullData_returnsNullData() {
            ApiResponse<Void> response = ApiResponse.fail("Failed", null);

            assertThat(response.success()).isFalse();
            assertEquals("Failed", response.message());
            assertNull(response.data());
        }
    }

    // =========================================================================
    // JSON serialization: @JsonInclude NON_NULL
    // =========================================================================
    @Nested
    class JsonSerialization {

        @Test
        void success_dataOnly_nullFieldsShouldBeExcluded() {
            // @JsonInclude(NON_NULL) ensures null fields are omitted in JSON serialization.
            // This test verifies the record is constructed correctly for that purpose.
            ApiResponse<String> response = ApiResponse.success("data");

            assertNotNull(response.success());
            assertNotNull(response.message());
            assertNotNull(response.data());
            assertNull(response.error()); // This should be omitted from JSON
            assertNotNull(response.timestamp());
        }

        @Test
        void error_messageOnly_nullFieldsShouldBeExcluded() {
            ApiResponse<Void> response = ApiResponse.error("Not found");

            assertNotNull(response.success());
            assertNotNull(response.message());
            assertNull(response.data());   // Will be omitted in JSON
            assertNull(response.error());  // Will be omitted in JSON
        }
    }
}
