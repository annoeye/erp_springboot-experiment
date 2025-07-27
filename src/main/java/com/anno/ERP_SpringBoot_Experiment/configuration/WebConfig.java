//package com.anno.ERP_SpringBoot_Experiment.configuration;
//
//import com.anno.ERP_SpringBoot_Experiment.converter.StringToDeviceInfoConverter;
//import lombok.AllArgsConstructor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.format.FormatterRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry; // Import này cần thiết
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//@AllArgsConstructor
//public class WebConfig implements WebMvcConfigurer {
//
//    private final StringToDeviceInfoConverter stringToDeviceInfoConverter;
//
//    @Override
//    public void addFormatters(FormatterRegistry registry) {
//        registry.addConverter(stringToDeviceInfoConverter);
//    }
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//
//        registry.addResourceHandler("/auth/assets/js/**")
//                .addResourceLocations("classpath:/static/js/");
//
//        registry.addResourceHandler("/auth/assets/css/**")
//                .addResourceLocations("classpath:/static/css/");
//
//        registry.addResourceHandler("/js/**")
//                .addResourceLocations("classpath:/static/js/");
//
//        registry.addResourceHandler("/css/**")
//                .addResourceLocations("classpath:/static/css/");
//
//        registry.addResourceHandler("/img/**")
//                .addResourceLocations("classpath:/static/img/");
//    }
//}