package org.criticizer.service.user;

import java.util.List;

//A generic class representing a paginated result set for user-related data
public class UserPageResult<T> {

    private final List<T> items;
    private final int totalItems;
    private final int currentPage;
    private final int pageSize;
    private final int totalPages;

    //Constructs a new UserPageResult with the specified items and pagination metadata.
    public UserPageResult(List<T> items, int totalItems, int currentPage, int pageSize) {
        this.items = items != null ? items : List.of();
        this.totalItems = totalItems;
        this.currentPage = currentPage;
        this.pageSize = pageSize;

        if (pageSize > 0) {
            this.totalPages = (int) Math.ceil((double) totalItems / pageSize);
        } else {
            this.totalPages = 0;
        }
    }

    public List<T> getItems() {
        return items;
    }


    public int getTotalItems() {
        return totalItems;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }
}