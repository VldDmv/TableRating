package org.criticizer.dto.helper;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Generic paginated response wrapper.
 *
 * @param <T> Type of items in the page
 */
public record PageResponse<T>(
        List<T> items, int currentPage, int totalPages, long totalItems, int pageSize) {
    public List<T> getItems() {
        return items();
    }

    public int getCurrentPage() {
        return currentPage();
    }

    public int getTotalPages() {
        return totalPages();
    }

    public long getTotalItems() {
        return totalItems();
    }

    public int getPageSize() {
        return pageSize();
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize());
    }

    public static <T> PageResponse<T> of(Page<?> page, List<T> items) {
        return new PageResponse<>(
                items,
                page.getNumber() + 1,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize());
    }

    public static <T> PageResponse<T> of(PageResponse<?> metadata, List<T> items) {
        return new PageResponse<>(
                items,
                metadata.currentPage(),
                metadata.totalPages(),
                metadata.totalItems(),
                metadata.pageSize());
    }
}
