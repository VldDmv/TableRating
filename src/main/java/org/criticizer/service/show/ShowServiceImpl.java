package org.criticizer.service.show;

import org.criticizer.dao.show.ShowDao;
import org.criticizer.entity.Show;
import org.criticizer.service.helper.ServiceValidator;
import org.criticizer.service.user.UserPageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Implementation of the ShowService interface for managing show-related operations.
 */
public class ShowServiceImpl implements ShowService {
    private static final Logger log = LoggerFactory.getLogger(ShowServiceImpl.class);
    private final ServiceValidator validator;
    private final ShowDao showDao;

    public ShowServiceImpl(ShowDao showDao, ServiceValidator validator) {
        this.showDao = showDao;
        this.validator = validator;
    }

    @Override
    public List<Show> getUserShows(int userId) {
        return showDao.getUserShows(userId);
    }

    @Override
    public boolean getShowStatus(String name, int userId) {
        return showDao.getShowStatus(name, userId);
    }

    @Override
    public void updateShowAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds) {
        validator.validateScore(newScore, userId, "Show");

        log.info("Updating show '{}' to '{}' with score {} for user {}", oldName, newName, newScore, userId);
        showDao.updateShowAndName(oldName, newName, newScore, userId, genreIds);
    }

    @Override
    public boolean isShowExists(String name, int userId) {
        return showDao.isShowExists(name, userId);
    }

    @Override
    public void addShow(String name, int userId, int score, List<Integer> genreIds) {
        validator.validateScore(score, userId, "Show");

        log.info("Adding show '{}' with score {} for user {}", name, score, userId);
        showDao.addShow(name, userId, score, genreIds);
    }

    @Override
    public UserPageResult<Show> getUserShowsPage(int userId, int page, int pageSize,
                                                 Integer genreId, String searchTerm,
                                                 String sortBy, String sortOrder) {
        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        List<Show> showsOnPage = showDao.findShowsByUserId(
                userId, genreId, sanitizedSearch,
                params.offset(), params.pageSize(),
                sortBy, sortOrder
        );

        int totalShows = showDao.countTotalForUser(userId, genreId, sanitizedSearch);

        return new UserPageResult<>(showsOnPage, totalShows, params.page(), params.pageSize());
    }

    @Override
    public void removeShow(String name, int userId) {
        log.info("Removing show '{}' for user {}", name, userId);
        showDao.removeShow(name, userId);
    }

    @Override
    public void toggleShowStatus(String name, int userId) {
        log.debug("Toggling status for show '{}' for user {}", name, userId);
        showDao.toggleShowStatus(name, userId);
    }
}