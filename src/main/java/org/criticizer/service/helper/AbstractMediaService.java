package org.criticizer.service.helper;

import org.criticizer.dto.helper.PageResponse;
import org.criticizer.exceptions.data.ItemAlreadyExistsException;
import org.criticizer.exceptions.data.ResourceNotFoundException;
import org.criticizer.repository.MediaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Abstract base service for all media entities (Game, Movie, Book, Show).
 * Contains common business logic to eliminate code duplication.
 *
 * @param <T> Entity type (Game, Movie, Book, Show)
 * @param <R> Response DTO type
 */
@Transactional(readOnly = true)
public abstract class AbstractMediaService<T extends MediaEntity, R> {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final MediaRepository<T> repository;
    protected final ServiceValidator validator;

    protected AbstractMediaService(MediaRepository<T> repository, ServiceValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    // ============= Abstract methods (must be implemented by subclasses) =============

    /**
     * Get entity name for logging and error messages.
     */
    protected abstract String getEntityName();

    /**
     * Create new entity instance.
     */
    protected abstract T createEntity(String name, String coverUrl, Integer userId, Integer score);

    /**
     * Assign categories (tags/genres) to entity.
     */
    protected abstract void assignCategories(T entity, List<Integer> categoryIds);

    /**
     * Convert entity to response DTO.
     */
    protected abstract R toResponse(T entity);

    // ============= Common CRUD operations =============

    /**
     * Get paginated items with filtering and search.
     */
    public PageResponse<T> getUserItemsPage(
            Integer userId, Integer page, Integer pageSize,
            Integer categoryId, String searchTerm,
            String sortBy, String sortOrder) {
        return getUserItemsPage(userId, page, pageSize, categoryId, searchTerm,
                sortBy, sortOrder, null, null, null);
    }

    /**
     * Get paginated items with filtering, search, score range and completion state.
     */
    public PageResponse<T> getUserItemsPage(
            Integer userId, Integer page, Integer pageSize,
            Integer categoryId, String searchTerm,
            String sortBy, String sortOrder,
            Integer minScore, Integer maxScore, Boolean completed) {

        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        Sort sort = createSort(sortBy, sortOrder);
        Pageable pageable = PageRequest.of(params.page() - 1, params.pageSize(), sort);

        //  Get only IDs with pagination (SQL-level LIMIT works correctly)
        Page<Integer> itemIds = repository.findItemIds(
                userId, categoryId, sanitizedSearch, minScore, maxScore, completed, pageable
        );

        //  Fetch full entities with categories using IDs
        List<T> items;
        if (!itemIds.isEmpty()) {
            List<T> fetched = repository.findByIdsWithCategories(itemIds.getContent());

            // Restore the order from the paginated ID list (SQL order is lost after IN query)
            java.util.Map<Integer, T> byId = fetched.stream()
                    .collect(java.util.stream.Collectors.toMap(MediaEntity::getId, e -> e));
            items = itemIds.getContent().stream()
                    .map(byId::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        } else {
            items = List.of();
        }

        log.debug("Fetched {} {}s (page {} of {}) for user {}",
                items.size(), getEntityName(),
                params.page(), itemIds.getTotalPages(), userId);

        //  Create PageImpl manually with items and pagination info from itemIds
        Page<T> itemPage = new org.springframework.data.domain.PageImpl<>(
                items,
                pageable,
                itemIds.getTotalElements()
        );


        return PageResponse.of(itemPage);
    }

    /**
     * Add new item for user.
     */
    @Transactional
    public void addItem(String name, String coverUrl, Integer userId, Integer score, List<Integer> categoryIds) {
        String trimmedName = validator.validateName(name, getEntityName() + " name");
        validator.validateScore(score, userId, getEntityName());

        if (repository.existsByNameIgnoreCaseAndUserId(trimmedName, userId)) {
            log.warn("Attempted to add duplicate {} '{}' for user {}",
                    getEntityName(), trimmedName, userId);
            throw new ItemAlreadyExistsException(getEntityName(), trimmedName);
        }

        T entity = createEntity(trimmedName, coverUrl, userId, score);


        T saved = repository.save(entity);


        if (categoryIds != null && !categoryIds.isEmpty()) {
            assignCategories(saved, categoryIds);
            repository.save(saved);
        }

        log.info("Added {} '{}' (ID: {}) for user {}",
                getEntityName(), saved.getName(), saved.getId(), userId);
    }

    /**
     * Update existing item.
     */
    @Transactional
    public void updateItem(String oldName, String newName, String coverUrl,
                           Integer score, Integer userId, List<Integer> categoryIds) {
        String trimmedOldName = validator.validateName(oldName, "Old " + getEntityName() + " name");
        String trimmedNewName = validator.validateName(newName, "New " + getEntityName() + " name");
        validator.validateScore(score, userId, getEntityName());

        T entity = repository.findByNameIgnoreCaseAndUserId(trimmedOldName, userId)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), trimmedOldName));

        // Check for name conflict
        if (!trimmedOldName.equalsIgnoreCase(trimmedNewName)) {
            if (repository.existsByNameIgnoreCaseAndUserId(trimmedNewName, userId)) {
                throw new ItemAlreadyExistsException(getEntityName(), trimmedNewName);
            }
        }

        entity.setName(trimmedNewName);
        entity.setCoverUrl(coverUrl);
        entity.setScore(score);

        if (categoryIds != null) {
            assignCategories(entity, categoryIds);
        }

        repository.save(entity);
        log.info("Updated {} '{}' to '{}' for user {}",
                getEntityName(), oldName, newName, userId);
    }

    /**
     * Remove item.
     */
    @Transactional
    public void removeItem(String name, Integer userId) {
        String trimmedName = validator.validateName(name, getEntityName() + " name");

        T entity = repository.findByNameIgnoreCaseAndUserId(trimmedName, userId)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), trimmedName));

        repository.delete(entity);
        log.info("Removed {} '{}' (ID: {}) for user {}",
                getEntityName(), entity.getName(), entity.getId(), userId);
    }

    /**
     * Toggle completion status.
     */
    @Transactional
    public R toggleStatus(String name, Integer userId) {
        String trimmedName = validator.validateName(name, getEntityName() + " name");

        T entity = repository.findByNameIgnoreCaseAndUserId(trimmedName, userId)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), trimmedName));

        entity.setCompleted(!entity.isCompleted());
        T updated = repository.save(entity);

        log.debug("Toggled completion status for {} '{}' to {}",
                getEntityName(), updated.getName(), updated.isCompleted());

        return toResponse(updated);
    }

    /**
     * Check if item exists.
     */
    public boolean isItemExists(String name, Integer userId) {
        String trimmedName = validator.validateName(name, getEntityName() + " name");
        return repository.existsByNameIgnoreCaseAndUserId(trimmedName, userId);
    }

    /**
     * Get item status.
     */
    public boolean getItemStatus(String name, Integer userId) {
        String trimmedName = validator.validateName(name, getEntityName() + " name");

        T entity = repository.findByNameIgnoreCaseAndUserId(trimmedName, userId)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), trimmedName));

        return entity.isCompleted();
    }

    /**
     * Updates cover URL for an item.
     *
     * @param name     Item name
     * @param coverUrl New cover URL (can be null/empty to remove)
     * @param userId   User ID
     */
    @Transactional
    public void updateCover(String name, String coverUrl, Integer userId) {
        String trimmedName = validator.validateName(name, getEntityName() + " name");

        T entity = repository.findByNameIgnoreCaseAndUserId(trimmedName, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        getEntityName(), trimmedName
                ));

        // Set cover URL (null/empty removes the cover)
        entity.setCoverUrl(coverUrl != null && !coverUrl.trim().isEmpty()
                ? coverUrl.trim()
                : null);

        repository.save(entity);

        log.info("Updated cover for {} '{}' (user {})",
                getEntityName(), name, userId);
    }
    // ============= Helper methods =============

    private Sort createSort(String sortBy, String sortOrder) {
        String column = (sortBy != null) ? sortBy : "name";

        if (!List.of("name", "score", "completed").contains(column)) {
            column = "name";
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return Sort.by(direction, column);
    }

    public abstract String getMediaType();

    /**
     * Returns a paginated page of response DTOs (not raw entities).
     */
    public PageResponse<R> getUserItemsPageAsDto(
            Integer userId, Integer page, Integer pageSize,
            Integer categoryId, String searchTerm,
            String sortBy, String sortOrder) {
        return getUserItemsPageAsDto(userId, page, pageSize, categoryId, searchTerm,
                sortBy, sortOrder, null, null, null);
    }

    /**
     * Returns a paginated page of DTOs with score range and completion filters.
     */
    public PageResponse<R> getUserItemsPageAsDto(
            Integer userId, Integer page, Integer pageSize,
            Integer categoryId, String searchTerm,
            String sortBy, String sortOrder,
            Integer minScore, Integer maxScore, Boolean completed) {

        PageResponse<T> entityPage = getUserItemsPage(
                userId, page, pageSize, categoryId, searchTerm, sortBy, sortOrder,
                minScore, maxScore, completed);

        List<R> dtos = entityPage.getItems().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                dtos,
                entityPage.getCurrentPage(),
                entityPage.getTotalPages(),
                entityPage.getTotalItems(),
                entityPage.getPageSize()
        );
    }
}