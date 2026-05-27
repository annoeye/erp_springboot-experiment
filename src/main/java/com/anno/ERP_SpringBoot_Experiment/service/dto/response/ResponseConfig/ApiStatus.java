package com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig;

import com.anno.ERP_SpringBoot_Experiment.config.Views;
import com.fasterxml.jackson.annotation.JsonView;

public class ApiStatus {
    @JsonView(Views.Public.class)
    private String message;

    @JsonView(Views.Public.class)
    private int code;

    public ApiStatus() {}

    public ApiStatus(int code) {
        this.code = code;
        this.message = "Success";
    }

    public ApiStatus(String message, int code) {
        this.message = message;
        this.code = code;
    }

    @JsonView(Views.Public.class)
    public String getMessage() {
        return message;
    }

    @JsonView(Views.Public.class)
    public int getCode() {
        return code;
    }

    public String name() {
        return message;
    }
}
