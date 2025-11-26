package org.criticizer.service.show;

import org.criticizer.entity.Show;
import org.criticizer.service.user.UserPageResult;

import java.util.List;

public interface ShowService {
    List<Show> getUserShows(int userId);

    void addShow(String name, int userId, int score, List<Integer> genreIds);

    void removeShow(String name, int userId);

    void toggleShowStatus(String name, int userId);

    void updateShowAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds);

    boolean isShowExists(String name, int userId);

    boolean getShowStatus(String name, int userId);

    UserPageResult<Show> getUserShowsPage(int userId, int page, int pageSize, Integer genreId, String searchTerm, String sortBy, String sortOrder);

}
