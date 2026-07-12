package com.anno.ERP_SpringBoot_Experiment.fineract.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.fineract.config.FineractProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FineractClientServiceTest {

    private RestClient restClient;
    private UserRepository userRepository;
    private FineractProperties fineractProperties;
    private FineractClientService clientService;
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        restClient = mock(RestClient.class);
        userRepository = mock(UserRepository.class);
        fineractProperties = mock(FineractProperties.class);
        clientService = new FineractClientService(restClient, userRepository, fineractProperties);
    }

    @Test
    void testGetOrCreateFineractClient_AlreadyExists() {
        User user = new User();
        user.setFineractClientId("99");

        String clientId = clientService.getOrCreateFineractClient(user);
        assertThat(clientId).isEqualTo("99");
        verifyNoInteractions(restClient);
    }

    @Test
    void testGetOrCreateFineractClient_CreatesClient() {
        User user = new User();
        user.setFullName("Nguyen Van A");
        user.setEmail("nva@example.com");

        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("clientId", "101");

        // Mock RestClient calls
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(any(String.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(mockResponse);

        String clientId = clientService.getOrCreateFineractClient(user);
        assertThat(clientId).isEqualTo("101");
        verify(userRepository, times(1)).save(user);
    }
}
