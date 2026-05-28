package org.criticizer.controller.admin;

import org.criticizer.dto.tag.CreateTagRequest;
import org.criticizer.dto.tag.UpdateTagRequest;
import org.criticizer.service.tag.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** MVC Controller for admin tag management. POST /admin/tags with ?action=add|update|delete */
@Controller
@RequestMapping("/admin/tags")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTagController {
    private static final Logger log = LoggerFactory.getLogger(AdminTagController.class);

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    public String handleTagAction(
            @RequestParam String action,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) String name,
            RedirectAttributes redirectAttributes) {

        try {
            switch (action) {
                case "add" -> {
                    tagService.createTag(new CreateTagRequest(name));
                    redirectAttributes.addFlashAttribute(
                            "flashSuccessMessage", "Tag added successfully");
                }
                case "update" -> {
                    tagService.updateTag(new UpdateTagRequest(id, name));
                    redirectAttributes.addFlashAttribute(
                            "flashSuccessMessage", "Tag updated successfully");
                }
                case "delete" -> {
                    tagService.deleteTag(id);
                    redirectAttributes.addFlashAttribute(
                            "flashSuccessMessage", "Tag deleted successfully");
                }
                default ->
                        redirectAttributes.addFlashAttribute(
                                "flashErrorMessage", "Invalid action: " + action);
            }
        } catch (org.criticizer.exceptions.data.ItemInUseException e) {
            log.warn("Cannot delete tag {}: still assigned to games", id);
            redirectAttributes.addFlashAttribute(
                    "flashErrorMessage", "Cannot delete tag: it is currently assigned to games");
        } catch (Exception e) {
            log.error("Error handling tag action '{}'", action, e);
            redirectAttributes.addFlashAttribute("flashErrorMessage", "Error: " + e.getMessage());
        }

        return "redirect:/admin/management?type=tags";
    }
}
