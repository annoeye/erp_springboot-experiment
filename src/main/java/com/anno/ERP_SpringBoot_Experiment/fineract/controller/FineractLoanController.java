package com.anno.ERP_SpringBoot_Experiment.fineract.controller;

import com.anno.ERP_SpringBoot_Experiment.fineract.service.FineractLoanService;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/erp/loans")
@RequiredArgsConstructor
public class FineractLoanController {

    private final FineractLoanService loanService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    @GetMapping
    public ResponseEntity<JsonNode> getLoans() {
        return ResponseEntity.ok(loanService.getLoans());
    }

    @GetMapping("/my")
    public ResponseEntity<JsonNode> getMyLoans(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(loanService.getLoansForUser(user));
    }

    @PostMapping("/my")
    public ResponseEntity<JsonNode> applyForLoan(@AuthenticationPrincipal UserDetails userDetails, @RequestBody JsonNode payload) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(loanService.applyLoanForUser(user, payload));
    }

    @PostMapping("/{loanId}/repayments")
    public ResponseEntity<JsonNode> repayLoan(@AuthenticationPrincipal UserDetails userDetails, @PathVariable Long loanId, @RequestBody JsonNode payload) {
        User user = getAuthenticatedUser(userDetails);
        return ResponseEntity.ok(loanService.repayLoanForUser(user, loanId, payload));
    }
}
