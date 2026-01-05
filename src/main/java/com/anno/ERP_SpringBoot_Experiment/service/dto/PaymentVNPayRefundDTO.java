package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentVNPayRefundDTO {

    @JsonProperty("vnp_Amount")
    Long amount;

    @JsonProperty("vnp_BankCode")
    String bankCode;

    @JsonProperty("vnp_BankTranNo")
    String bankTranNo;

    @JsonProperty("vnp_CardType")
    String transactionType;

    @JsonProperty("vnp_OrderInfo")
    String orderId;

    @JsonProperty("vnp_PayDate")
    String payDate;

    @JsonProperty("vnp_ResponseCode")
    int responseCode;

    @JsonProperty("vnp_TmnCode")
    String tmnCode;

    @JsonProperty("vnp_TransactionNo")
    String transactionNo;

    @JsonProperty("vnp_TransactionStatus")
    int transactionStatus;

    @JsonProperty("vnp_TxnRef")
    String txnRef;

    @JsonProperty("vnp_SecureHash")
    String secureHash;

    @JsonProperty("ip_address")
    String ipAddress;
}
