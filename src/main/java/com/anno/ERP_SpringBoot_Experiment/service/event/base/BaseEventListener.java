package com.anno.ERP_SpringBoot_Experiment.service.event.base;

import com.anno.ERP_SpringBoot_Experiment.repository.RefreshTokenRepository;
import com.anno.ERP_SpringBoot_Experiment.service.EmailService;
import com.anno.ERP_SpringBoot_Experiment.service.JwtService;
import com.anno.ERP_SpringBoot_Experiment.service.UserDetailsServiceImpl;
import com.anno.ERP_SpringBoot_Experiment.service.UserService.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@RequiredArgsConstructor
public abstract class BaseEventListener {
    protected final EmailService emailService;
    protected final RefreshTokenRepository refreshTokenRepository;
    protected final Helper helper;
    protected final JwtService jwtService;
    protected final UserDetailsServiceImpl userDetailsService;

    @Value("${server.port}")
    protected String serverPort;
}
