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

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        if (!model.containsAttribute("userLogin")) {
            model.addAttribute("userLogin", new UserLogin());
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        if (!model.containsAttribute("userRegister")) {
            model.addAttribute("userRegister", new UserRegister());
        }
        return "auth/register";
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

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("userRegister") UserRegister userRegisterDetails,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!userRegisterDetails.isPasswordConfirmed()) {
            bindingResult.rejectValue("confirmPassword", "error.userRegister", "Mật khẩu xác nhận không khớp.");
        }
        if (bindingResult.hasErrors()) {
            // model.addAttribute("userRegister", userRegisterDetails); // Không cần thiết
            return "auth/register";
        }

        try {
            String resultMessage = userService.createUser(userRegisterDetails);
            redirectAttributes.addFlashAttribute("registrationSuccess", resultMessage);
            return "redirect:/auth/login"; // Chuyển hướng đến trang đăng nhập sau khi đăng ký thành công
        } catch (CustomException e) {
            // model.addAttribute("userRegister", userRegisterDetails); // Không cần thiết
            if (e.getStatus() == HttpStatus.CONFLICT) { // Email hoặc username đã tồn tại
                // Cố gắng xác định lỗi cụ thể hơn nếu có thể từ message của exception
                if (e.getMessage().toLowerCase().contains("email")) {
                    bindingResult.rejectValue("email", "error.userRegister", e.getMessage());
                } else if (e.getMessage().toLowerCase().contains("tên đăng nhập")) {
                    bindingResult.rejectValue("userName", "error.userRegister", e.getMessage());
                } else {
                    model.addAttribute("registrationError", e.getMessage());
                }
            } else {
                model.addAttribute("registrationError", e.getMessage());
            }
            return "auth/register";
        } catch (MessagingException e) {
            // model.addAttribute("userRegister", userRegisterDetails); // Không cần thiết
            model.addAttribute("registrationError", "Lỗi gửi email xác thực. Vui lòng thử lại hoặc liên hệ quản trị viên.");
            return "auth/register";
        } catch (Exception e) {
            // model.addAttribute("userRegister", userRegisterDetails); // Không cần thiết
            model.addAttribute("registrationError", "Đã có lỗi không mong muốn xảy ra trong quá trình đăng ký.");
            return "auth/register";
        }
    }
}
