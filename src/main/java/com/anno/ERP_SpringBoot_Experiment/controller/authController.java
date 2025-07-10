package com.anno.ERP_SpringBoot_Experiment.controller;


import com.anno.ERP_SpringBoot_Experiment.exception.CustomException;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserLogin;
import com.anno.ERP_SpringBoot_Experiment.model.dto.UserRegister;
import com.anno.ERP_SpringBoot_Experiment.response.AuthResponse;
import com.anno.ERP_SpringBoot_Experiment.service.implementation.iUser;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@AllArgsConstructor
public class authController {

    private final iUser userService;
    private static final Logger logger = LoggerFactory.getLogger(authController.class);

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("userLogin")) {
            model.addAttribute("userLogin", new UserLogin());
        }
        return "pages/sign-in";
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        if (!model.containsAttribute("userRegister")) {
            model.addAttribute("userRegister", new UserRegister());
        }
        return "pages/sign-up";
    }

    @PostMapping("/login")
    public String processLoginMvc(
            @Valid @ModelAttribute("userLogin") UserLogin userLoginDetails,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
            ){
        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error ->
                    System.err.println("Validation Error: " + error.getDefaultMessage())
            );
            return "auth/login";
        }
        try {
            AuthResponse authResponse = userService.loginUser(userLoginDetails);
            session.setAttribute("accessToken", authResponse.getAccessToken());
            session.setAttribute("refreshToken", authResponse.getRefreshToken());
            session.setAttribute("username", authResponse.getUsername());
            session.setAttribute("email", authResponse.getEmail());
            session.setAttribute("userId", authResponse.getUserId());
            session.setAttribute("roles", authResponse.getRoles());
            session.setAttribute("phoneNumber", authResponse.getPhoneNumber());
            session.setAttribute("gender", authResponse.getGender());
            session.setAttribute("avatarUrl", authResponse.getAvatarUrl());
            redirectAttributes.addFlashAttribute("loginSuccess", "Đăng nhập thành công!");
            return "auth/register";
        } catch (MessagingException e) {
            model.addAttribute("loginError", "Lỗi gửi email xác thực. Vui lòng thử lại.");
            return "auth/login";
        } catch (RuntimeException e) {
            model.addAttribute("userLogin", userLoginDetails);
            model.addAttribute("loginError", "Tên đăng nhập hoặc mật khẩu không đúng, hoặc đã có lỗi xảy ra.");
            return "auth/login";
        }
    }


    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("userRegister") UserRegister userRegisterDetails,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!userRegisterDetails.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "PasswordMismatch", "Mật khẩu xác nhận không khớp.");
        }
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            String resultMessage = userService.createUser(userRegisterDetails);
            redirectAttributes.addFlashAttribute("registrationSuccess", resultMessage);
            return "redirect:/auth/login";
        } catch (CustomException e) {
            logger.warn("CustomException during registration for email {}: {} - Status: {}", userRegisterDetails.getEmail(), e.getMessage(), e.getStatus());
            // model.addAttribute("userRegister", userRegisterDetails); // Không cần thiết
            if (e.getStatus() == HttpStatus.CONFLICT) {
                if (e.getMessage().toLowerCase().contains("email")) {
                    bindingResult.rejectValue("email", "DuplicateValue", e.getMessage());
                } else if (e.getMessage().toLowerCase().contains("tên đăng nhập")) {
                    bindingResult.rejectValue("userName", "DuplicateValue", e.getMessage());
                } else {
                    model.addAttribute("registrationError", e.getMessage());
                }
            } else {
                model.addAttribute("registrationError", e.getMessage());
            }
            return "auth/register";
        } catch (MessagingException e) {
            logger.error("MessagingException during registration for email {}: {}", userRegisterDetails.getEmail(), e.getMessage(), e);
            // model.addAttribute("userRegister", userRegisterDetails);
            model.addAttribute("registrationError", "Lỗi gửi email xác thực. Vui lòng thử lại hoặc liên hệ quản trị viên.");
            return "auth/register";
        } catch (Exception e) { // Bắt các Exception chung khác
            logger.error("Unexpected Exception during registration for email {}: {}", userRegisterDetails.getEmail(), e.getMessage(), e);
            // model.addAttribute("userRegister", userRegisterDetails);
            model.addAttribute("registrationError", "Đã có lỗi không mong muốn xảy ra trong quá trình đăng ký.");
            return "auth/register";
        }
    }

    @GetMapping("/verify")
    public String verifyAccount(@RequestParam("token") String token,
                                @RequestParam("username") String username,
                                @RequestParam("type") String type,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        try {
            String verificationMessage = userService.verifyAccount(username, token, type);
            // Nếu xác thực thành công, chuyển hướng đến trang đăng nhập với thông báo thành công
            redirectAttributes.addFlashAttribute("verificationSuccess", verificationMessage);
            return "redirect:/auth/login";
        } catch (CustomException e) {
            logger.warn("CustomException during account verification for username {}: {} - Status: {}", username, e.getMessage(), e.getStatus());
            // Nếu có lỗi, hiển thị thông báo lỗi trên trang đăng nhập (hoặc một trang thông báo lỗi riêng)
            // Ở đây, chúng ta sẽ thêm lỗi vào redirectAttributes để hiển thị trên trang login
            redirectAttributes.addFlashAttribute("verificationError", e.getMessage());
            return "redirect:/auth/login"; // Hoặc bạn có thể trả về một view lỗi cụ thể
            // return "auth/verification-failed";
        } catch (Exception e) {
            logger.error("Unexpected exception during account verification for username {}: {}", username, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("verificationError", "Đã có lỗi không mong muốn xảy ra trong quá trình xác thực.");
            return "redirect:/auth/login"; // Hoặc một view lỗi chung
        }
    }
}
