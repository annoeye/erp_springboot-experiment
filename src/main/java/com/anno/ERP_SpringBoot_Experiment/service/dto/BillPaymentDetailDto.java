package com.anno.ERP_SpringBoot_Experiment.service.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.BillPaymentDetail}
 */
@Value
public class BillPaymentDetailDto implements Serializable {
    String paymentMethod;
    String paymentStatus;
    LocalDateTime paymentDate;
    String bankName;
    String bankAccountNumber;
    String bankTranNo;
    Integer installmentTerm;
    Double installmentMonthlyAmount;
    String installmentPartner;
    String paymentNotes;
}