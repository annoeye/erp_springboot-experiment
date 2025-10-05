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
                    map
                )
            );
        }

        if (Objects.equals(ex.getErrorCode(), AppConstant.BAD_REQUEST.getCode())) {
            return badRequest(
                new ErrorResponse(
                    AppConstant.BAD_REQUEST.getCode(),
                    AppConstant.BAD_REQUEST.getMessage(),
                    map
                )
            );
        }

        return internalServerError(
                        new ErrorResponse(
                        AppConstant.SERVICE_ERROR.getCode(),
                        AppConstant.SERVICE_ERROR.getMessage(),
                        map
                )
        );
    }


    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("service", ex.getMessage());
        return forbidden(
                new ErrorResponse(AppConstant.FORBIDDEN.getCode(), AppConstant.FORBIDDEN.getMessage(), map)
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> map = new HashMap<>();
        map.put("service", ex.getMessage());
        return unauthorized(
                new ErrorResponse(AppConstant.UNAUTHORIZED.getCode(), AppConstant.UNAUTHORIZED.getMessage(), map)
        );
    }

}
