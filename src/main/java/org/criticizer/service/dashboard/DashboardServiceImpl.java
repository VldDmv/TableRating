package org.criticizer.service.dashboard;

import org.criticizer.dao.user.UserDao;
import org.criticizer.entity.AdminStats;

/**
 * Implementation of the DashboardService interface that provides administrative statistics.
 */
public class DashboardServiceImpl implements DashboardService {

    private final UserDao userDao;

    /**
     * Constructs a new DashboardServiceImpl with the specified UserDao.
     */
    public DashboardServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Retrieves statistics for the admin dashboard
     */
    @Override
    public AdminStats getAdminDashboardStats() {
        return userDao.getAdminStatistics();
    }
}