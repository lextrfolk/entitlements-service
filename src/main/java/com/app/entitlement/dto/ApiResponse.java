package com.app.entitlement.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.Map;

/**
 * A generic API response wrapper to standardize all responses.
 *
 * @param <T> the type of the responseData returned
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message (success/failure reason)", example = "Upload completed successfully")
    private String message;

    @Schema(description = "response/data returned by the API")
    private T responseData;

    @Schema(description = "Error code for failed operations", example = "ERR_VALIDATION_FAILED")
    private String errorCode;

    @Builder.Default
    @Schema(description = "Timestamp of the response", example = "2025-04-05T12:34:56.789Z")
    private Instant responseTime = Instant.now();

    @Schema(description = "Any additional metadata to include")
    private Map<String, Object> extra;

    @Schema(description = "HTTP status code to return", example = "200")
    private Integer httpStatusCode;

    /**
     * Creates a success response with a message and responseData.
     *
     * @param message the success message
     * @param responseData the data returned
     * @param <T>     the type of responseData
     * @return an ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(String message, T responseData) {
        return success(message, responseData, null, 200);
    }

    /**
     * Creates a success response with message, responseData, and optional extra data.
     *
     * @param message the success message
     * @param responseData the data returned
     * @param extra   optional metadata (can be null)
     * @param <T>     the type of responseData
     * @return an ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(String message, T responseData, Map<String, Object> extra) {
        return success(message, responseData, extra, 200);
    }

    /**
     * Creates a success response with full control over all fields.
     *
     * @param message        the success message
     * @param responseData        the data returned
     * @param extra          optional metadata
     * @param httpStatusCode the HTTP status code to return
     * @param <T>            the type of responseData
     * @return an ApiResponse with success=true
     */
    public static <T> ApiResponse<T> success(String message, T responseData, Map<String, Object> extra, int httpStatusCode) {
        return buildResponse(true, message, responseData, null, extra, httpStatusCode);
    }

    /**
     * Creates a failure response with a message and error code.
     *
     * @param message   the failure message
     * @param errorCode a business-defined error code
     * @param <T>       the type of responseData
     * @return an ApiResponse with success=false
     */
    public static <T> ApiResponse<T> failure(String message, String errorCode) {
        return failure(message, errorCode, null, 400);
    }

    /**
     * Creates a failure response with message, error code, and extra metadata.
     *
     * @param message   the failure message
     * @param errorCode a business-defined error code
     * @param extra     optional metadata (can be null)
     * @param <T>       the type of responseData
     * @return an ApiResponse with success=false
     */
    public static <T> ApiResponse<T> failure(String message, String errorCode, Map<String, Object> extra) {
        return failure(message, errorCode, extra, 400);
    }

    /**
     * Creates a failure response with full control over fields.
     *
     * @param message        the failure message
     * @param errorCode      a business-defined error code
     * @param extra          optional metadata
     * @param httpStatusCode HTTP status code to return (e.g. 400, 500)
     * @param <T>            the type of responseData
     * @return an ApiResponse with success=false
     */
    public static <T> ApiResponse<T> failure(String message, String errorCode, Map<String, Object> extra, int httpStatusCode) {
        return buildResponse(false, message, null, errorCode, extra, httpStatusCode);
    }

    /**
     * Internal shared builder used by both success and failure methods.
     *
     * @param success        true for success, false for error
     * @param message        response message
     * @param responseData        responseData object
     * @param errorCode      error code if failure
     * @param extra          optional metadata
     * @param httpStatusCode HTTP status to return
     * @param <T>            responseData type
     * @return constructed ApiResponse
     */
    private static <T> ApiResponse<T> buildResponse(
            boolean success,
            String message,
            T responseData,
            String errorCode,
            Map<String, Object> extra,
            int httpStatusCode
    ) {
        return ApiResponse.<T>builder()
                .success(success)
                .message(message)
                .responseData(responseData)
                .errorCode(errorCode)
                .extra(extra)
                .httpStatusCode(httpStatusCode)
                .build();
    }
}
