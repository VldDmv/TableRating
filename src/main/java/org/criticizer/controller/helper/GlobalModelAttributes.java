package org.criticizer.controller.helper;

import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final SecurityUtil securityUtil;

    public GlobalModelAttributes(SecurityUtil securityUtil) {
        this.securityUtil = securityUtil;
    }

    @ModelAttribute("user")
    public User populateUser() {
        try {
            return securityUtil.getCurrentUser();
        } catch (Exception e) {
            return null;
        }
    }
}
