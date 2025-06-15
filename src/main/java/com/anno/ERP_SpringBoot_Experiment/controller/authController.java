package com.anno.ERP_SpringBoot_Experiment.controller;


import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.logging.Handler;

@Controller
@RequestMapping("/auth")
@AllArgsConstructor
public class authController {

    private final iUser userService;

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("userLogin")) {
            model.addAttribute("userLogin", new UserLogin());
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLoginMvc(
            @Valid @ModelAttribute("userLogin") UserLogin userLoginDetails,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
            ){
        if (bindingResult.hasErrors()) return "auth/login";
        try {
            AuthResponse authResponse = userService.loginUser(userLoginDetails);
            session.setAttribute("accessToken", authResponse.getAccessToken());
            session.setAttribute("refreshToken", authResponse.getRefreshToken());
            session.setAttribute("username", authResponse.getUsername());
            session.setAttribute("email", authResponse.getEmail());
            session.setAttribute("userId", authResponse.getUserId());
            session.setAttribute("roles", authResponse.getRoles());
            redirectAttributes.addFlashAttribute("loginSuccess", "Đăng nhập thành công!");
            return "redirect:/dashboard";
        } catch (MessagingException e) {
            model.addAttribute("loginError", "Lỗi gửi email xác thực. Vui lòng thử lại.");
            return "auth/login";
        }catch (RuntimeException e) {
            model.addAttribute("userLogin", userLoginDetails);
            model.addAttribute("loginError", "Tên đăng nhập hoặc mật khẩu không đúng, hoặc đã có lỗi xảy ra.");
            return "auth/login";
        }
    }
}
