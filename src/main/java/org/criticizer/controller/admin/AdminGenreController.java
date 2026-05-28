package org.criticizer.controller.admin;

import java.util.Arrays;
import java.util.List;
import org.criticizer.dto.genre.CreateGenreRequest;
import org.criticizer.dto.genre.UpdateGenreRequest;
import org.criticizer.service.genre.GenreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC Controller for admin genre management operations. Handles form-based genre operations
 * (create, update, delete). All endpoints require ADMIN role.
 */
@Controller
@RequestMapping("/admin/genres")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenreController {
    private static final Logger log = LoggerFactory.getLogger(AdminGenreController.class);

    private final GenreService genreService;

    public AdminGenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    /** Single endpoint for all actions. */
    @PostMapping
    public String handleGenreAction(
            @RequestParam String action,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String[] mediaTypes,
            RedirectAttributes redirectAttributes) {

        try {
            List<String> mediaTypeList = mediaTypes != null ? Arrays.asList(mediaTypes) : List.of();

            switch (action) {
                case "add" -> {
                    CreateGenreRequest createReq = new CreateGenreRequest(name, mediaTypeList);
                    genreService.createGenre(createReq);
                    redirectAttributes.addFlashAttribute(
                            "flashSuccessMessage", "Genre added successfully");
                }
                case "update" -> {
                    UpdateGenreRequest updateReq = new UpdateGenreRequest(id, name, mediaTypeList);
                    genreService.updateGenre(updateReq);
                    redirectAttributes.addFlashAttribute(
                            "flashSuccessMessage", "Genre updated successfully");
                }
                case "delete" -> {
                    genreService.deleteGenre(id);
                    redirectAttributes.addFlashAttribute(
                            "flashSuccessMessage", "Genre deleted successfully");
                }
                default -> {
                    redirectAttributes.addFlashAttribute("flashErrorMessage", "Invalid action");
                }
            }
        } catch (org.criticizer.exceptions.data.ItemInUseException e) {
            log.warn("Cannot perform action on genre: in use", e);
            redirectAttributes.addFlashAttribute(
                    "flashErrorMessage", "Cannot delete genre: it is currently in use");
        } catch (Exception e) {
            log.error("Error handling genre action", e);
            redirectAttributes.addFlashAttribute("flashErrorMessage", "Error: " + e.getMessage());
        }

        return "redirect:/admin/management?type=genres";
    }
}
