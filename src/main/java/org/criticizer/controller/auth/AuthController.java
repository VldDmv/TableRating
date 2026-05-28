package org.criticizer.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.criticizer.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/auth/register")
    public String registerForm(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.info("Form Registration attempt for user: {}", username);

        // 1. Manual validation for missing parameters
        if (username == null || password == null || confirmPassword == null) {
            redirectAttributes.addFlashAttribute("flashErrorMessage", "All fields are required");
            return "redirect:/index?showRegister=true";
        }

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("flashErrorMessage", "Passwords do not match");
            return "redirect:/index?showRegister=true";
        }

        try {
            userService.registerUser(username, password);

            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(username, password));
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext);

            log.info("User '{}' registered and logged in successfully", username);
            return "redirect:/dashboard";
        } catch (Exception e) {
            log.error("Registration failed for user: {}", username, e);
            redirectAttributes.addFlashAttribute(
                    "flashErrorMessage", "Registration failed. Please try again.");
            return "redirect:/index?showRegister=true";
        }
    }

    /** Check if username is available (AJAX endpoint) Returns JSON: {"available": true/false} */
    @GetMapping("/auth/check-username")
    @ResponseBody
    public java.util.Map<String, Boolean> checkUsernameAvailability(
            @RequestParam("username") String username) {

        log.debug("Checking username availability: {}", username);

        // Validate username format
        if (username == null || username.trim().isEmpty()) {
            return java.util.Map.of("available", false);
        }

        String trimmedUsername = username.trim();

        // Check if username is already taken
        boolean isAvailable = !userService.existsByUsername(trimmedUsername);

        log.debug("Username '{}' available: {}", trimmedUsername, isAvailable);

        return java.util.Map.of("available", isAvailable);
    }
}
