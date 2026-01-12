package com.anno.ERP_SpringBoot_Experiment.web.rest.error;

import com.anno.ERP_SpringBoot_Experiment.common.constants.AppConstant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class ExceptionTranslator {
    private ResponseEntity<ErrorResponse> badRequest(ErrorResponse result) {
        return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ErrorResponse> internalServerError(ErrorResponse result) {
        return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> notFound(ErrorResponse result) {
        return new ResponseEntity<>(result, HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<ErrorResponse> forbidden(ErrorResponse result) {
        return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<ErrorResponse> unauthorized(ErrorResponse result) {
        return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("service", ex.getMessage());

        if (Objects.equals(ex.getErrorCode(), AppConstant.NOT_FOUND.getCode())) {
            return notFound(
                    new ErrorResponse(
                            AppConstant.NOT_FOUND.getCode(),
                            AppConstant.NOT_FOUND.getMessage(),
                            map));
        }

        if (Objects.equals(ex.getErrorCode(), AppConstant.BAD_REQUEST.getCode())) {
            return badRequest(
                    new ErrorResponse(
                            AppConstant.BAD_REQUEST.getCode(),
                            AppConstant.BAD_REQUEST.getMessage(),
                            map));
        }

        return internalServerError(
                new ErrorResponse(
                        AppConstant.SERVICE_ERROR.getCode(),
                        AppConstant.SERVICE_ERROR.getMessage(),
                        map));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("service", ex.getMessage());
        return forbidden(
                new ErrorResponse(AppConstant.FORBIDDEN.getCode(), AppConstant.FORBIDDEN.getMessage(), map));
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("service", ex.getMessage());
        return unauthorized(
                new ErrorResponse(AppConstant.UNAUTHORIZED.getCode(), AppConstant.UNAUTHORIZED.getMessage(), map));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ErrorResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n⚠️ VALIDATION ERROR [").append(ex.getBindingResult().getObjectName()).append("]\n");

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField();
            String value = error.getRejectedValue() != null ? error.getRejectedValue().toString() : "null";
            String message = error.getDefaultMessage();

            logMessage.append(" ").append(field)
                    .append(" = '").append(truncate(value, 30)).append("'")
                    .append(" → ").append(message).append("\n");

            fieldErrors.put(field, message);
        });

        org.slf4j.LoggerFactory.getLogger(ExceptionTranslator.class).warn(logMessage.toString());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fields", fieldErrors);

        return badRequest(new ErrorResponse(AppConstant.BAD_REQUEST.getCode(), "Validation failed", errorDetails));
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n⚠️ CONSTRAINT VIOLATION\n");

        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String value = violation.getInvalidValue() != null ? violation.getInvalidValue().toString() : "null";
            String message = violation.getMessage();

            logMessage.append("   ├─ ").append(path)
                    .append(" = '").append(truncate(value, 30)).append("'")
                    .append(" → ").append(message).append("\n");

            fieldErrors.put(path, message);
        });

        org.slf4j.LoggerFactory.getLogger(ExceptionTranslator.class).warn(logMessage.toString());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fields", fieldErrors);

        return badRequest(new ErrorResponse(AppConstant.BAD_REQUEST.getCode(), "Constraint violation", errorDetails));
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ResponseEntity<ErrorResponse> handleBindException(org.springframework.validation.BindException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("\n⚠️ BIND ERROR [").append(ex.getBindingResult().getObjectName()).append("]\n");

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String field = error.getField();
            String value = error.getRejectedValue() != null ? error.getRejectedValue().toString() : "null";
            String message = error.getDefaultMessage();

            logMessage.append("   ├─ ").append(field)
                    .append(" = '").append(truncate(value, 30)).append("'")
                    .append(" → ").append(message).append("\n");

            fieldErrors.put(field, message);
        });

        org.slf4j.LoggerFactory.getLogger(ExceptionTranslator.class).warn(logMessage.toString());

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("fields", fieldErrors);

        return badRequest(new ErrorResponse(AppConstant.BAD_REQUEST.getCode(), "Binding failed", errorDetails));
    }

    private String truncate(String str, int maxLength) {
        if (str == null)
            return "null";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

}
