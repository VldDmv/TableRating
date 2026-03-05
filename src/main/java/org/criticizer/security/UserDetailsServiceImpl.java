package org.criticizer.security;

import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.service.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService implementation.
 * Returns AuthenticatedUser which holds the full User entity,
 * allowing SecurityUtil to retrieve the user without an extra DB query.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            User user = userService.getUser(username);
            if (user == null) {
                throw new UsernameNotFoundException("User not found: " + username);
            }
            return new AuthenticatedUser(user);
        } catch (UserNotFoundException e) {
            throw new UsernameNotFoundException("User not found: " + username, e);
        }
    }
}