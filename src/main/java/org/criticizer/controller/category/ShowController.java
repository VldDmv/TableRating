package org.criticizer.controller.category;

import org.criticizer.controller.helper.AbstractMediaController;
import org.criticizer.dto.show.CreateShowRequest;
import org.criticizer.dto.show.ShowResponse;
import org.criticizer.dto.show.UpdateShowRequest;
import org.criticizer.entity.Show;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.show.ShowService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shows")
public class ShowController extends AbstractMediaController<
        Show,
        ShowResponse,
        CreateShowRequest,
        UpdateShowRequest> {

    public ShowController(ShowService showService, SecurityUtil securityUtil) {
        super(showService, securityUtil);
    }

    @Override
    protected String getEntityName() {
        return "Show";
    }

    @Override
    protected ShowResponse convertToResponse(Show entity) {
        return ShowResponse.from(entity);
    }
}