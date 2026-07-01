package com.anno.ERP_SpringBoot_Experiment.fineract.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FineractLoanService {

    private final RestClient fineractRestClient;
    private final FineractClientService clientService;

    public JsonNode getLoans() {
        return fineractRestClient.get()
                .uri("/loans")
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getLoansForUser(User user) {
        String clientId = clientService.getOrCreateFineractClient(user);
        return fineractRestClient.get()
                .uri("/loans?clientId={clientId}", clientId)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode applyLoanForUser(User user, JsonNode payload) {
        String clientId = clientService.getOrCreateFineractClient(user);
        
        // Force the Fineract Client ID in the payload
        if (payload.isObject()) {
            ((ObjectNode) payload).put("clientId", Long.parseLong(clientId));
        }

        return fineractRestClient.post()
                .uri("/loans")
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode repayLoanForUser(User user, Long loanId, JsonNode payload) {
        // Enforce lazy sync registration checks
        clientService.getOrCreateFineractClient(user);

        return fineractRestClient.post()
                .uri("/loans/{loanId}/transactions?command=repayment", loanId)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);
    }
}
