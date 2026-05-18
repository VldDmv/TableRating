package org.criticizer.controller.helper;

import jakarta.validation.Valid;
import org.criticizer.dto.helper.*;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.MediaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Abstract base controller for all media REST endpoints.
 * Works with regular interfaces.
 */
public abstract class AbstractMediaController<
        T extends MediaEntity,
        R,
        C extends CreateMediaRequest,
        U extends UpdateMediaRequest
        > {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final AbstractMediaService<T, R> service;
    protected final SecurityUtil securityUtil;

    protected AbstractMediaController(
            AbstractMediaService<T, R> service,
            SecurityUtil securityUtil) {
        this.service = service;
        this.securityUtil = securityUtil;
    }

    // ============= Abstract methods =============

    protected abstract String getEntityName();
    protected abstract R convertToResponse(T entity);

    // ============= Common REST endpoints =============

    @GetMapping
    public ResponseEntity<PageResponse<R>> getUserItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder) {

        User currentUser = securityUtil.getCurrentUser();
        log.debug("Getting {}s for user {} - page: {}, size: {}, categoryId: {}",
                getEntityName(), currentUser.getName(), page, size, categoryId);

        PageResponse<T> result = service.getUserItemsPage(
                currentUser.getId(),
                page + 1,
                size,
                categoryId,
                search,
                sortBy,
                sortOrder
        );

        List<R> responses = result.getItems().stream()
                .map(this::convertToResponse)
                .toList();

        Page<R> pageResult = new PageImpl<>(
                responses,
                PageRequest.of(page, size),
                result.getTotalItems()
        );

        return ResponseEntity.ok(PageResponse.of(pageResult));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> createItem(@Valid @RequestBody C request) {
        User currentUser = securityUtil.getCurrentUser();
        log.info("User {} creating {}: {}",
                currentUser.getName(), getEntityName(), request.name());

        service.addItem(
                request.name(),
                request.coverUrl(),
                currentUser.getId(),
                request.score(),
                request.categoryIds() != null ? request.categoryIds() : List.of()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MessageResponse(getEntityName() + " created successfully"));
    }

    @PutMapping("/{name}")
    public ResponseEntity<MessageResponse> updateItem(
            @PathVariable String name,
            @Valid @RequestBody U request) {

        User currentUser = securityUtil.getCurrentUser();
        log.info("User {} updating {} '{}' to '{}'",
                currentUser.getName(), getEntityName(), name, request.name());

        service.updateItem(
                name,
                request.name(),
                request.coverUrl(),
                request.score(),
                currentUser.getId(),
                request.categoryIds() != null ? request.categoryIds() : List.of()
        );

        return ResponseEntity.ok(
                new MessageResponse(getEntityName() + " updated successfully")
        );
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<MessageResponse> deleteItem(@PathVariable String name) {
        User currentUser = securityUtil.getCurrentUser();
        log.info("User {} deleting {}: {}", currentUser.getName(), getEntityName(), name);

        service.removeItem(name, currentUser.getId());

        return ResponseEntity.ok(
                new MessageResponse(getEntityName() + " deleted successfully")
        );
    }

    @PatchMapping("/{name}/toggle")
    public ResponseEntity<StatusToggleResponse> toggleItemStatus(@PathVariable String name) {
        User currentUser = securityUtil.getCurrentUser();
        log.debug("User {} toggling status for {}: {}",
                currentUser.getName(), getEntityName(), name);

        service.toggleStatus(name, currentUser.getId());
        boolean newStatus = service.getItemStatus(name, currentUser.getId());

        return ResponseEntity.ok(
                new StatusToggleResponse(
                        getEntityName() + " status toggled successfully",
                        newStatus
                )
        );
    }

    @GetMapping("/{name}/status")
    public ResponseEntity<StatusResponse> getItemStatus(@PathVariable String name) {
        User currentUser = securityUtil.getCurrentUser();
        boolean status = service.getItemStatus(name, currentUser.getId());
        return ResponseEntity.ok(new StatusResponse(status));
    }

    @GetMapping("/{name}/exists")
    public ResponseEntity<ExistsResponse> checkItemExists(@PathVariable String name) {
        User currentUser = securityUtil.getCurrentUser();
        boolean exists = service.isItemExists(name, currentUser.getId());
        return ResponseEntity.ok(new ExistsResponse(exists));
    }

    /**
     * Updates pipeline status (NONE / WISHLIST / BACKLOG / DROPPED).
     * PATCH /api/{entityType}/{name}/pipeline
     */
    @PatchMapping("/{name}/pipeline")
    public ResponseEntity<MessageResponse> updatePipelineStatus(
            @PathVariable String name,
            @RequestBody Map<String, String> request) {

        User currentUser = securityUtil.getCurrentUser();
        String status = request.get("status");
        log.info("User {} setting status of {} '{}' to {}",
                currentUser.getName(), getEntityName(), name, status);

        service.updateStatus(name, status, currentUser.getId());
        return ResponseEntity.ok(
                new MessageResponse(getEntityName() + " status updated"));
    }
    /**
     * Updates cover URL for an item.
     * PATCH /api/{entityType}/{name}/cover
     */
    @PatchMapping("/{name}/cover")
    public ResponseEntity<MessageResponse> updateCover(
            @PathVariable String name,
            @RequestBody Map<String, String> request) {

        User currentUser = securityUtil.getCurrentUser();
        String coverUrl = request.get("coverUrl");

        log.info("User {} updating cover for {} '{}'",
                currentUser.getName(), getEntityName(), name);

        service.updateCover(name, coverUrl, currentUser.getId());

        return ResponseEntity.ok(
                new MessageResponse("Cover updated successfully")
        );
    }
}