
package com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<T> {

    ApiStatus status;
    T data;

    public static <T> Response<T> ok(T data) {
        final ApiStatus status = new ApiStatus(HttpStatus.OK.value());
        return Response.<T>builder()
                .status(status)
                .data(data)
                .build();
    }

    public static <T> Response<T> ok(T data, String message) {
        final ApiStatus status = new ApiStatus(message, HttpStatus.OK.value());
        return Response.<T>builder()
                .status(status)
                .data(data)
                .build();
    }

    public static <T> Response<T> ok(String message) {
        final ApiStatus status = new ApiStatus(message, HttpStatus.OK.value());
        return Response.<T>builder()
                .status(status)
                .build();
    }

    public static <T> Response<T> created(T data) {
        final ApiStatus status = new ApiStatus(HttpStatus.CREATED.value());
        return Response.<T>builder()
                .status(status)
                .data(data)
                .build();
    }

    public static <T> Response<T> fail(String message, int code) {
        ApiStatus status = new ApiStatus(message, code);
        return Response.<T>builder()
                .status(status)
                .build();
    }

    public static <T> Response<T> found(T data) {
        final ApiStatus status = new ApiStatus(HttpStatus.FOUND.value());
        return Response.<T>builder()
                .status(status)
                .build();
    }

    public static <T> Response<T> fail(ApiStatus status) {
        return Response.<T>builder()
                .status(status)
                .build();
    }

    public static <T> Response<T> noContent() {
        ApiStatus status = new ApiStatus(HttpStatus.NO_CONTENT.value());
        return Response.<T>builder()
                .status(status)
                .build();
    }

    @JsonIgnore
    public String getMessage() {
        return status != null ? status.getMessage() : null;
    }
}
