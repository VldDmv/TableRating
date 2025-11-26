package org.criticizer.dao.show;

import org.criticizer.entity.Show;

import java.util.List;

public interface ShowDao {

    List<Show> getUserShows(int userId);

    int addShow(String name, int userId, int score, List<Integer> genreIds);

    void removeShow(String name, int userId);

    void toggleShowStatus(String name, int userId);

    void updateShowAndName(String oldName, String newName, int newScore, int userId, List<Integer> genreIds);

    boolean isShowExists(String name, int userId);

    boolean getShowStatus(String name, int userId);

    void deleteShowsByUserId(int userId);

    int countTotal();

    List<Show> findShowsByUserId(int userId, Integer genreId, String searchTerm, int offset, int limit, String sortBy, String sortOrder);

    int countTotalForUser(int userId, Integer genreId, String searchTerm);
}

