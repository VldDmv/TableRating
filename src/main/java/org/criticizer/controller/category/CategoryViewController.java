package org.criticizer.controller.category;

import java.util.Map;
import org.criticizer.constants.ContentCategory;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.genre.GenreService;
import org.criticizer.service.helper.AbstractMediaService;
import org.criticizer.service.helper.MediaTypeResolver;
import org.criticizer.service.tag.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** View controller for category pages (games, movies, books, shows). */
@Controller
public class CategoryViewController {

    private static final Logger log = LoggerFactory.getLogger(CategoryViewController.class);

    private final MediaTypeResolver mediaTypeResolver;
    private final TagService tagService;
    private final GenreService genreService;
    private final SecurityUtil securityUtil;

    public CategoryViewController(
            MediaTypeResolver mediaTypeResolver,
            TagService tagService,
            GenreService genreService,
            SecurityUtil securityUtil) {
        this.mediaTypeResolver = mediaTypeResolver;
        this.tagService = tagService;
        this.genreService = genreService;
        this.securityUtil = securityUtil;
    }

    @GetMapping("/{mediaType:games|movies|books|shows}")
    public String categoryPage(@PathVariable String mediaType, Model model) {

        ContentCategory type = ContentCategory.fromString(mediaType);
        User currentUser = securityUtil.getCurrentUser();

        log.debug("Loading category page: {} for user: {}", mediaType, currentUser.getName());

        AbstractMediaService<?, ?> service = mediaTypeResolver.resolve(type);

        model.addAttribute("entityType", type.getPlural());
        model.addAttribute("entityNameSingular", type.getDisplayName());
        model.addAttribute("entityNamePlural", type.getDisplayPlural());
        model.addAttribute("addFormId", "add-" + type.getSingular() + "-form");

        if (type == ContentCategory.GAMES) {
            model.addAttribute("allTags", tagService.getAllTags());
        } else {
            model.addAttribute("allGenres", genreService.getAvailableGenresFor(type.getSingular()));
        }

        model.addAttribute("paramNames", getStandardParamNames());

        var initialData =
                service.getUserItemsPage(currentUser.getId(), 1, 10, null, null, "name", "asc");

        model.addAttribute("initialData", initialData);

        return "category/listRatingsTemplate";
    }

    private Map<String, String> getStandardParamNames() {
        return Map.of(
                "addItemName", "name",
                "addItemScore", "score",
                "addItemTagIds", "selectedIds");
    }
}
