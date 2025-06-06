package com.anno.ERP_SpringBoot_Experiment.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class MvcGlobalExceptionHandler {

    private final static Logger logger = LoggerFactory.getLogger(MvcGlobalExceptionHandler.class);
    private final static String DEFAULT_ERROR_VIEW = "error-custom";

    @ExceptionHandler(CustomException.class)
    public ModelAndView handleCustomerException(
            CustomException ex,
            HttpServletRequest request,
            HttpServletResponse httpResponse
    ) {
        logger.error("MVC Error: {} for path: {}",
                ex.getMessage(),
                request.getRequestURL(),
                ex
        );

        HttpStatus status = ex.getStatus();
        httpResponse.setStatus(status.value());
        ModelAndView mav = new ModelAndView(DEFAULT_ERROR_VIEW);
        mav.addObject("timestamp", System.currentTimeMillis());
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", ex.getMessage());
        mav.addObject("path", request.getRequestURL());

        return mav;
    }

}
