package com.anno.ERP_SpringBoot_Experiment.config;

import com.anno.ERP_SpringBoot_Experiment.web.rest.impl.merchandiseControllerImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Tự động áp dụng {@link com.fasterxml.jackson.annotation.JsonView} phù hợp
 * dựa trên role của user hiện tại cho các response từ merchandise controller.
 */
@ControllerAdvice(assignableTypes = merchandiseControllerImpl.class)
@RequiredArgsConstructor
public class JsonViewAdvice implements ResponseBodyAdvice<Object> {

    private final ViewResolver viewResolver;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        if (body instanceof MappingJacksonValue) {
            return body;
        }

        // Xác định view class dựa trên role
        Class<?> viewClass = viewResolver.resolveViewClass();

        MappingJacksonValue value = new MappingJacksonValue(body);
        value.setSerializationView(viewClass);

        return value;
    }
}
