package com.anno.ERP_SpringBoot_Experiment.service.dto.request;
import lombok.Data;
@Data
public class UpdateAdminNotesRequest {
    private String orderId;
    private String adminNotes;
    private String notes;
}
