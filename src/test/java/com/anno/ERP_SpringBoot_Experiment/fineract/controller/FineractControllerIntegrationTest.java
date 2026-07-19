package com.anno.ERP_SpringBoot_Experiment.fineract.controller;

import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractClientService;
import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractLoanProductService;
import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractLoanService;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FineractControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FineractLoanService loanService;

    @MockBean
    private FineractClientService clientService;

    @MockBean
    private FineractLoanProductService loanProductService;

    @MockBean
    private com.anno.ERP_SpringBoot_Experiment.service.MinioService minioService;

    @MockBean
    private com.anno.ERP_SpringBoot_Experiment.service.EmailService emailService;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    @WithMockUser(username = "john.doe@example.com")
    void testGetMyLoans() throws Exception {
        User user = new User();
        user.setEmail("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/erp/loans/my"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "john.doe@example.com")
    void testSyncClient() throws Exception {
        User user = new User();
        user.setEmail("john.doe@example.com");
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(clientService.getOrCreateFineractClient(any())).thenReturn("101");

        mockMvc.perform(post("/api/v1/erp/clients/sync"))
                .andExpect(status().isOk());
    }
}
