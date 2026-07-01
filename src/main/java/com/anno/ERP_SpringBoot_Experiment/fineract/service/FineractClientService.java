package com.anno.ERP_SpringBoot_Experiment.fineract.service;

import com.anno.ERP_SpringBoot_Experiment.fineract.dto.FineractClientCreateRequestDTO;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class FineractClientService {

    private final RestClient fineractRestClient;
    private final UserRepository userRepository;

    public JsonNode getClients() {
        return fineractRestClient.get()
                .uri("/clients")
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode createClient(FineractClientCreateRequestDTO request) {
        // Set defaults as specified
        if (request.getOfficeId() == null) request.setOfficeId(1L);
        if (request.getActive() == null) request.setActive(true);
        if (request.getActivationDate() == null) request.setActivationDate("01 January 2026");
        if (request.getDateFormat() == null) request.setDateFormat("dd MMMM yyyy");
        if (request.getLocale() == null) request.setLocale("en");

        return fineractRestClient.post()
                .uri("/clients")
                .body(request)
                .retrieve()
                .body(JsonNode.class);
    }

    @Transactional
    public String getOrCreateFineractClient(User user) {
        if (user.getFineractClientId() != null) {
            return user.getFineractClientId();
        }

        String fullName = user.getFullName() != null ? user.getFullName() : "ERP User";
        String[] nameParts = fullName.trim().split("\\s+");
        String firstname = nameParts[0];
        String lastname = nameParts.length > 1 ? fullName.substring(firstname.length()).trim() : "LastName";

        FineractClientCreateRequestDTO syncRequest = new FineractClientCreateRequestDTO(
                firstname,
                lastname,
                1L,
                true,
                "01 January 2026",
                "dd MMMM yyyy",
                "en"
        );

        JsonNode response = createClient(syncRequest);
        if (response != null && response.has("clientId")) {
            String clientId = response.get("clientId").asText();
            user.setFineractClientId(clientId);
            userRepository.save(user);
            return clientId;
        }

        throw new RuntimeException("Không thể đồng bộ/tạo Client trên Fineract");
    }
}
