package com.anno.ERP_SpringBoot_Experiment.fineract.controller;

import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractLoanProductService;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FineractLoanProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FineractLoanProductService fineractLoanProductService;

    @InjectMocks
    private FineractLoanProductController fineractLoanProductController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fineractLoanProductController).build();
    }

    @Test
    void getLoanProducts_ShouldReturnOk() throws Exception {
        JsonNode mockNode = JsonNodeFactory.instance.arrayNode();
        when(fineractLoanProductService.getLoanProducts()).thenReturn(mockNode);

        mockMvc.perform(get("/api/v1/erp/loan-products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
