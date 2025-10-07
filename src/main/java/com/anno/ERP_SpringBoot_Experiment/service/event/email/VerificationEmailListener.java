package com.anno.ERP_SpringBoot_Experiment.service.event.email;

import com.anno.ERP_SpringBoot_Experiment.domainevents.VerificationEmailEvent;
import com.anno.ERP_SpringBoot_Experiment.service.EmailService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.UserDetailsServiceImpl;
import com.anno.ERP_SpringBoot_Experiment.service.event.base.BaseEventListener;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class VerificationEmailListener extends BaseEventListener {

    public VerificationEmailListener(EmailService emailService,
                                     JwtService jwtService,
                                     UserDetailsServiceImpl userDetailsService,
                                     @Value("${server.port}") String serverPort) {
        super(emailService, jwtService, userDetailsService);
        this.serverPort = serverPort;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationEmail(VerificationEmailEvent body) {
        try {
            String verificationUrl = "http://localhost:" + serverPort + "/api/auth/verify?token="
                    + body.emailVerificationToken() + "&purpose=" + body.purpose();

            emailService.sendVerificationEmail(
                    body.email(),
                    body.username(),
                    verificationUrl
            );
        } catch (MessagingException e) {
            log.error("Gửi email xác thực thất bại cho user: {}", body.username(), e);
        }
    }
}
