package org.criticizer.dao.user;

import org.criticizer.entity.AdminStats;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;

import java.util.List;

public interface UserDao {
    User findUserByName(String name);

    void addUser(User user);

    List<User> getAllUsers();

    void updateUserRole(int userId, Role newRole);

    List<User> findUsers(String searchTerm, int offset, int limit, boolean publicOnly);

    int countUsers(String searchTerm, boolean publicOnly);

    void deleteUser(int userId);

    void updateUserPrivacy(int userId, boolean isPublic);

    AdminStats getAdminStatistics();
}
