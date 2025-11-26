package org.criticizer.service.user;

import org.criticizer.entity.Role;
import org.criticizer.entity.User;

import java.util.List;

public interface UserService {
    User getUser(String name);

    void registerUser(String name, String password);

    int getUserId(String name);

    List<User> listAllUsers();

    void changeUserRole(int targetUserId, Role newRole, User initiator);

    UserPageResult<User> getUsersPage(String searchTerm, int page, int pageSize, boolean publicOnly);

    void deleteUser(int targetUserId, User initiator);

    void updateUserPrivacy(int userId, boolean isPublic);

    User authenticate(String name, String password);
}
