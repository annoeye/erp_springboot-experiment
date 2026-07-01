package com.anno.ERP_SpringBoot_Experiment.fineract.controller;

import com.anno.ERP_SpringBoot_Experiment.fineract.dto.FineractClientCreateRequestDTO;
import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FineractClientControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FineractClientService fineractClientService;

    @InjectMocks
    private FineractClientController fineractClientController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fineractClientController).build();
    }

    @Test
    void getClients_ShouldReturnOk() throws Exception {
        JsonNode mockNode = JsonNodeFactory.instance.objectNode().put("totalFilteredRecords", 1);
        when(fineractClientService.getClients()).thenReturn(mockNode);

        mockMvc.perform(get("/api/v1/erp/clients")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void createClient_ShouldReturnOk() throws Exception {
        FineractClientCreateRequestDTO request = new FineractClientCreateRequestDTO();
        
        JsonNode mockNode = JsonNodeFactory.instance.objectNode().put("clientId", 1);
        when(fineractClientService.createClient(any(FineractClientCreateRequestDTO.class))).thenReturn(mockNode);

        mockMvc.perform(post("/api/v1/erp/clients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
