package com.anno.ERP_SpringBoot_Experiment.fineract.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FineractLoanServiceTest {

    private RestClient restClient;
    private FineractClientService clientService;
    private FineractLoanService loanService;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        clientService = mock(FineractClientService.class);
        loanService = new FineractLoanService(restClient, clientService);
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

        ObjectNode payload = mapper.createObjectNode();
        payload.put("amount", 5000);

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
        assertThat(payload.get("clientId").asLong()).isEqualTo(123L);
    }

    @Test
    void testRepayLoanForUser() {
        User user = new User();
        user.setFineractClientId("123");
        when(clientService.getOrCreateFineractClient(user)).thenReturn("123");

        ObjectNode payload = mapper.createObjectNode();
        payload.put("transactionAmount", 500);

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
