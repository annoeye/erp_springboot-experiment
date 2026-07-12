package com.anno.ERP_SpringBoot_Experiment.fineract.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.fineract.config.FineractProperties;
import com.anno.ERP_SpringBoot_Experiment.fineract.dto.LoanApplicationRequestDTO;
import com.anno.ERP_SpringBoot_Experiment.fineract.dto.LoanRepaymentRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FineractLoanServiceTest {

    private RestClient restClient;
    private FineractClientService clientService;
    private FineractProperties fineractProperties;
    private FineractLoanService loanService;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        clientService = mock(FineractClientService.class);
        fineractProperties = mock(FineractProperties.class);
        loanService = new FineractLoanService(restClient, clientService, fineractProperties);
    }

    @Test
    void testGetLoansForUser() {
        User user = new User();
        user.setFineractClientId("123");

        when(clientService.getOrCreateFineractClient(user)).thenReturn("123");

        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/loans?clientId={clientId}"), eq("123"))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("totalFilteredRecords", 1);
        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        JsonNode result = loanService.getLoansForUser(user);
        assertThat(result).isNotNull();
        assertThat(result.get("totalFilteredRecords").asInt()).isEqualTo(1);
    }

    @Test
    void testApplyLoanForUser() {
        User user = new User();
        user.setFineractClientId("123");
        when(clientService.getOrCreateFineractClient(user)).thenReturn("123");

        LoanApplicationRequestDTO payload = new LoanApplicationRequestDTO();
        payload.setPrincipal(BigDecimal.valueOf(5000));

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/loans"))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("loanId", 12);
        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        JsonNode result = loanService.applyLoanForUser(user, payload);
        assertThat(result.get("loanId").asInt()).isEqualTo(12);
    }

    @Test
    void testRepayLoanForUser() {
        User user = new User();
        user.setFineractClientId("123");
        when(clientService.getOrCreateFineractClient(user)).thenReturn("123");

        LoanRepaymentRequestDTO payload = new LoanRepaymentRequestDTO();
        payload.setTransactionAmount(BigDecimal.valueOf(500));
        payload.setNote("Repayment for loan");

        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(eq("/loans/{loanId}/transactions?command=repayment"), eq(12L))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("resourceId", 999);
        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        JsonNode result = loanService.repayLoanForUser(user, 12L, payload);
        assertThat(result.get("resourceId").asInt()).isEqualTo(999);
    }
}
