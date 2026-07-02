package org.criticizer.controller.api;

import org.criticizer.dto.ioport.ExportRow;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.ioport.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exports a user's collection per category as a CSV download.
 *
 * <p>CSV columns: name, score, completed, coverUrl, categories
 * (categories are semicolon-separated to coexist with comma column delimiters).
 */
@RestController
@RequestMapping("/api")
public class ExportController {

    private static final String[] HEADERS = {"name", "score", "completed", "coverUrl", "categories"};

    private final ExportService service;
    private final SecurityUtil securityUtil;

    public ExportController(ExportService service, SecurityUtil securityUtil) {
        this.service = service;
        this.securityUtil = securityUtil;
    }

    @GetMapping(value = "/export/{category}.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv(@PathVariable String category) {
        User user = securityUtil.getCurrentUser();
        List<ExportRow> rows = service.exportItems(category, user.getId());

        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (ExportRow row : rows) {
            sb.append(csvField(row.name())).append(',')
                    .append(row.score() == null ? "" : row.score()).append(',')
                    .append(row.completed()).append(',')
                    .append(csvField(row.coverUrl())).append(',')
                    .append(csvField(String.join(";", row.categories())))
                    .append('\n');
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename(user.getName(), category) + "\"")
                .body(sb.toString());
    }

    private String csvField(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n");
        if (!needsQuotes) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String filename(String username, String category) {
        String safe = (username == null ? "user" : username).replaceAll("[^A-Za-z0-9._-]", "_");
        return safe + "-" + category + ".csv";
    }
}
