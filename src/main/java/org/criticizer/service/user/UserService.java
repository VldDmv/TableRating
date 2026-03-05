package org.criticizer.service.user;

import org.criticizer.dto.helper.PageResponse;
import org.criticizer.dto.user.UserPublicResponse;
import org.criticizer.entity.Role;
import org.criticizer.entity.User;
import org.criticizer.exceptions.data.UserAlreadyExistsException;
import org.criticizer.exceptions.data.UserNotFoundException;
import org.criticizer.exceptions.security.InsufficientPermissionsException;
import org.criticizer.exceptions.security.OperationNotPermittedException;
import org.criticizer.exceptions.validation.InvalidInputException;
import org.criticizer.repository.*;
import org.criticizer.service.helper.ServiceValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for managing users.
 */
@Service
@Transactional(readOnly = true)
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final MovieRepository movieRepository;
    private final BookRepository bookRepository;
    private final ShowRepository showRepository;
    private final ServiceValidator validator;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       GameRepository gameRepository,
                       MovieRepository movieRepository,
                       BookRepository bookRepository,
                       ShowRepository showRepository,
                       ServiceValidator validator,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.movieRepository = movieRepository;
        this.bookRepository = bookRepository;
        this.showRepository = showRepository;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
    }

    public User getUser(String name) {
        return userRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new UserNotFoundException(name));
    }

    /**
     * Get user by ID — uses a direct DB lookup, not in-memory filtering.
     */
    public User getUserById(int id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public int getUserId(String name) {
        return getUser(name).getId();
    }

    @Transactional
    public void registerUser(String name, String password) {
        String trimmedName = validator.validateUsername(name);
        validator.validatePassword(password);

        if (userRepository.existsByNameIgnoreCase(trimmedName)) {
            log.warn("Registration failed: user '{}' already exists", trimmedName);
            throw new UserAlreadyExistsException(trimmedName);
        }

        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(trimmedName, hashedPassword);
        user.setRole(Role.USER);
        user.setProfileIsPublic(false);

        userRepository.save(user);
        log.info("User '{}' registered successfully", trimmedName);
    }

    public boolean existsByUsername(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return userRepository.existsByNameIgnoreCase(name.trim());
    }

    public List<User> listAllUsers() {
        log.debug("Listing all users");
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public PageResponse<User> getUsersPage(String searchTerm, int page, int pageSize, boolean publicOnly) {
        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        Pageable pageable = PageRequest.of(params.page() - 1, params.pageSize(),
                Sort.by(Sort.Direction.ASC, "name"));

        Page<User> userPage = (sanitizedSearch != null && !sanitizedSearch.isEmpty())
                ? userRepository.searchUsers(sanitizedSearch, publicOnly, pageable)
                : userRepository.findUsers(publicOnly, pageable);

        log.debug("Fetched {} users (page {} of {})",
                userPage.getContent().size(), params.page(), userPage.getTotalPages());

        return PageResponse.of(userPage);
    }

    /**
     * Returns paginated public users with media statistics.
     * Uses a single aggregated query per media type (4 queries total)
     * instead of 4 queries per user, avoiding N+1.
     */
    public PageResponse<UserPublicResponse> getUsersPageWithStats(
            String searchTerm,
            int page,
            int pageSize,
            String sortBy,
            String sortOrder) {

        ServiceValidator.PaginationParams params = validator.validatePagination(page, pageSize);
        String sanitizedSearch = validator.sanitizeSearchTerm(searchTerm);

        Pageable unpaged = PageRequest.of(0, Integer.MAX_VALUE, Sort.by("name"));
        List<User> allPublicUsers = (sanitizedSearch != null && !sanitizedSearch.isEmpty())
                ? userRepository.searchUsers(sanitizedSearch, true, unpaged).getContent()
                : userRepository.findUsers(true, unpaged).getContent();

        if (allPublicUsers.isEmpty()) {
            return new PageResponse<>(List.of(), params.page(), 0, 0L, params.pageSize());
        }

        List<Integer> userIds = allPublicUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Map<Integer, Long> gamesCountMap = gameRepository.countByUserIds(userIds);
        Map<Integer, Long> moviesCountMap = movieRepository.countByUserIds(userIds);
        Map<Integer, Long> booksCountMap = bookRepository.countByUserIds(userIds);
        Map<Integer, Long> showsCountMap = showRepository.countByUserIds(userIds);

        List<UserPublicResponse> usersWithStats = allPublicUsers.stream()
                .map(user -> toPublicResponse(user, gamesCountMap, moviesCountMap,
                        booksCountMap, showsCountMap))
                .collect(Collectors.toList());

        usersWithStats = sortUsersByStats(usersWithStats, sortBy, sortOrder);

        int totalItems = usersWithStats.size();
        int totalPages = (int) Math.ceil((double) totalItems / params.pageSize());
        int startIndex = (params.page() - 1) * params.pageSize();
        int endIndex = Math.min(startIndex + params.pageSize(), totalItems);

        List<UserPublicResponse> pagedUsers = startIndex < totalItems
                ? usersWithStats.subList(startIndex, endIndex)
                : List.of();

        log.debug("Fetched {} users with stats (page {} of {})",
                pagedUsers.size(), params.page(), totalPages);

        return new PageResponse<>(pagedUsers, params.page(), totalPages,
                (long) totalItems, params.pageSize());
    }

    @Transactional
    public void changeUserRole(int targetUserId, Role newRole, User initiator) {
        if (newRole == null) {
            throw new InvalidInputException("newRole", "cannot be null");
        }

        if (initiator == null || initiator.getRole() != Role.ADMIN) {
            log.warn("Unauthorised role change attempt by user {}",
                    initiator != null ? initiator.getId() : "null");
            throw new InsufficientPermissionsException("ADMIN");
        }

        if (initiator.getId() == targetUserId && newRole == Role.USER) {
            throw new OperationNotPermittedException(
                    "changeUserRole",
                    "Administrator cannot remove their own admin role"
            );
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        log.info("Admin '{}' changed role for user {} to {}",
                initiator.getName(), targetUserId, newRole);
    }

    @Transactional
    public void deleteUser(int targetUserId, User initiator) {
        if (initiator == null || initiator.getRole() != Role.ADMIN) {
            throw new InsufficientPermissionsException("ADMIN");
        }

        if (initiator.getId() == targetUserId) {
            throw new OperationNotPermittedException("deleteUser",
                    "You cannot delete your own account");
        }

        if (!userRepository.existsById(targetUserId)) {
            throw new UserNotFoundException(targetUserId);
        }

        log.info("Admin '{}' deleting user ID {}", initiator.getName(), targetUserId);

        gameRepository.deleteByUserId(targetUserId);
        movieRepository.deleteByUserId(targetUserId);
        bookRepository.deleteByUserId(targetUserId);
        showRepository.deleteByUserId(targetUserId);
        userRepository.deleteById(targetUserId);

        log.info("Deleted user ID {} and all associated media", targetUserId);
    }

    @Transactional
    public void updateUserPrivacy(int userId, boolean isPublic) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setProfileIsPublic(isPublic);
        userRepository.save(user);

        log.info("Updated privacy for user {} to: {}", userId, isPublic ? "public" : "private");
    }

    // ============= Private helpers =============

    private UserPublicResponse toPublicResponse(
            User user,
            Map<Integer, Long> gamesCountMap,
            Map<Integer, Long> moviesCountMap,
            Map<Integer, Long> booksCountMap,
            Map<Integer, Long> showsCountMap) {

        int gamesCount = gamesCountMap.getOrDefault(user.getId(), 0L).intValue();
        int moviesCount = moviesCountMap.getOrDefault(user.getId(), 0L).intValue();
        int booksCount = booksCountMap.getOrDefault(user.getId(), 0L).intValue();
        int showsCount = showsCountMap.getOrDefault(user.getId(), 0L).intValue();
        int totalItems = gamesCount + moviesCount + booksCount + showsCount;

        return new UserPublicResponse(
                user.getName(),
                gamesCount,
                moviesCount,
                booksCount,
                showsCount,
                totalItems,
                user.isProfileIsPublic(),
                user.getCreatedAt()
        );
    }

    private List<UserPublicResponse> sortUsersByStats(
            List<UserPublicResponse> users,
            String sortBy,
            String sortOrder) {

        Comparator<UserPublicResponse> comparator = switch (sortBy != null ? sortBy.toLowerCase() : "name") {
            case "gamescount" -> Comparator.comparingInt(UserPublicResponse::getGamesCount);
            case "moviescount" -> Comparator.comparingInt(UserPublicResponse::getMoviesCount);
            case "bookscount" -> Comparator.comparingInt(UserPublicResponse::getBooksCount);
            case "showscount" -> Comparator.comparingInt(UserPublicResponse::getShowsCount);
            case "totalitems" -> Comparator.comparingInt(UserPublicResponse::getTotalItems);
            default -> Comparator.comparing(UserPublicResponse::getName, String.CASE_INSENSITIVE_ORDER);
        };

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        return users.stream().sorted(comparator).collect(Collectors.toList());
    }
}