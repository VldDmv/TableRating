package org.criticizer.controller.user;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * View controller for Users page.
 * Simplified version - all data loading via REST API.
 */
@Controller
@RequestMapping("/users")
public class UsersViewController {

    /**
     * Displays the users directory page.
     * All data is loaded via AJAX using /api/users endpoint.
     *
     * @return users page template
     */
    @GetMapping
    public String showUsersPage() {
        return "users/users";
    }
}