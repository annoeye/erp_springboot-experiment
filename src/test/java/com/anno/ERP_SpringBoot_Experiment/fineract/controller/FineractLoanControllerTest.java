package com.anno.ERP_SpringBoot_Experiment.fineract.controller;

import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractLoanService;
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
class FineractLoanControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FineractLoanService fineractLoanService;

    @InjectMocks
    private FineractLoanController fineractLoanController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fineractLoanController).build();
    }

    @Test
    void getLoans_ShouldReturnOk() throws Exception {
        JsonNode mockNode = JsonNodeFactory.instance.objectNode().put("totalFilteredRecords", 1);
        when(fineractLoanService.getLoans()).thenReturn(mockNode);

        mockMvc.perform(get("/api/v1/erp/loans")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
