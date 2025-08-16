package com.anno.ERP_SpringBoot_Experiment.controller.apiRequest;

import com.anno.ERP_SpringBoot_Experiment.service.UserActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/user")
public class userActionController {
    @Autowired
    private UserActionService userActionService;

    @GetMapping("/Create-Link")
    public String createRegistrationLink(@RequestParam List<String> roleName) {
        return userActionService.createAccount(roleName);
    }
}
