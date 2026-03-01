package com.anno.ERP_SpringBoot_Experiment.service.event.email;

import com.anno.ERP_SpringBoot_Experiment.domainevent.SendCodeResetPassword;
import com.anno.ERP_SpringBoot_Experiment.service.EmailService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.UserDetails.UserDetailsServiceImpl;
import com.anno.ERP_SpringBoot_Experiment.service.event.base.BaseEventListener;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class ResetPasswordListener extends BaseEventListener {

    public ResetPasswordListener(EmailService emailService,
                                 JwtService jwtService,
                                 UserDetailsServiceImpl userDetailsService,
                                 @Value("${server.port}") String serverPort) {
        super(emailService, jwtService, userDetailsService);
        this.serverPort = serverPort;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSendCodeResetPassword(SendCodeResetPassword body) throws MessagingException {
        try {
            emailService.sendPasswordResetOtpEmail(body.user().getEmail(), body.user().getUsername(), body.code());
            log.info("Đã gửi mã OTP đặt lại mật khẩu cho user: {}", body.user().getUsername());
        } catch (MessagingException e) {
            log.error("Lỗi gửi email OTP đặt lại mật khẩu cho {}: {}", body.user().getEmail(), e.getMessage(), e);
            throw e;
        }
    }
}
