package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.entity.CreateAccount;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Role;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Log;
import com.anno.ERP_SpringBoot_Experiment.repository.CreateAccountRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.RoleRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserActionLogRepository;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUserAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserActionService implements iUserAction {
    @Autowired
    private UserActionLogRepository userActionLogRepository;
    @Autowired
    private CreateAccountRepository createAccountRepository;
    @Autowired
    private RoleRepository roleRepository;
    private static final Logger logger = LoggerFactory.getLogger(UserActionService.class);
    private final String URL_FE = "http://localhost:5173/berry-vue/register";

    @Override
    public void log(User user, Log.ActionType action, String targetId, String description, String targetType) {
        Log log = Log.builder()
                .user(user)
                .actionType(action)
                .targetId(targetId)
                .targetType(targetType)
                .createdAt(LocalDateTime.now())
                .description(description)
                .build();
        userActionLogRepository.save(log);
    }

    @Override
    public String createAccount(List<String> roleName) {
        List<Role> roles = roleRepository.findByRoleIn(roleName);

        if (roles.size() != roleName.size()) {
            throw new CustomException("Một hoặc nhiều chức vụ không tồn tại.", HttpStatus.NOT_FOUND);
        }

        CreateAccount createAccount = CreateAccount
                .builder()
                .token(UUID.randomUUID().toString())
                .endTime(LocalDateTime.now().plusDays(1))
                .roles(roles)
                .build();

        createAccountRepository.save(createAccount);
        logger.info("Tạo đường dẫn đăng ký thành công.");
        return URL_FE + "?token=" + createAccount.getToken();
    }

}
