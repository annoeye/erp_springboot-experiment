package com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig;

public class ApiStatus {
    private String message;
    private int code;

    public ApiStatus(int code) {
        this.code = code;
        this.message = "Success";
    }

    public ApiStatus(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public String name() {
        return message;
    }
}
