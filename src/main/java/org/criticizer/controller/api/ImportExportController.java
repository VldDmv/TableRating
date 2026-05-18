package org.criticizer.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.criticizer.dto.ioport.ExportRow;
import org.criticizer.dto.ioport.ImportResult;
import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.ioport.ImportExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Export and import a user's collection per category in CSV or JSON.
 *
 * <p>CSV columns: name, score, completed, coverUrl, categories
 * (categories are semicolon-separated to coexist with comma column delimiters).
 */
@RestController
@RequestMapping("/api")
public class ImportExportController {

    private static final Logger log = LoggerFactory.getLogger(ImportExportController.class);

    private static final String[] HEADERS = {"name", "score", "completed", "coverUrl", "categories"};

    private final ImportExportService service;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;

    public ImportExportController(ImportExportService service,
                                  SecurityUtil securityUtil,
                                  ObjectMapper objectMapper) {
        this.service = service;
        this.securityUtil = securityUtil;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/export/{category}.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ExportRow>> exportJson(@PathVariable String category) {
        User user = securityUtil.getCurrentUser();
        List<ExportRow> rows = service.exportItems(category, user.getId());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename(user.getName(), category, "json") + "\"")
                .body(rows);
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
                        "attachment; filename=\"" + filename(user.getName(), category, "csv") + "\"")
                .body(sb.toString());
    }

    @PostMapping(value = "/import/{category}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResult> importFile(@PathVariable String category,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        User user = securityUtil.getCurrentUser();

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ImportResult(0, 0, List.of("Empty file")));
        }

        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        List<ExportRow> rows = name.endsWith(".csv")
                ? parseCsv(file)
                : parseJson(file);

        log.info("Importing {} rows of {} for user {}", rows.size(), category, user.getName());
        return ResponseEntity.ok(service.importItems(category, user.getId(), rows));
    }

    private List<ExportRow> parseJson(MultipartFile file) throws IOException {
        return objectMapper.readValue(file.getInputStream(), new TypeReference<List<ExportRow>>() {});
    }

    private List<ExportRow> parseCsv(MultipartFile file) throws IOException {
        List<ExportRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null) return rows;

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cols = splitCsvLine(line);
                if (cols.size() < 3) continue;

                String itemName = cols.get(0);
                Integer score = parseIntOrNull(cols.get(1));
                boolean completed = "true".equalsIgnoreCase(cols.get(2).trim());
                String coverUrl = cols.size() > 3 ? cols.get(3) : null;
                List<String> cats = cols.size() > 4 && !cols.get(4).isBlank()
                        ? Arrays.stream(cols.get(4).split(";"))
                                .map(String::trim).filter(s -> !s.isEmpty()).toList()
                        : List.of();
                rows.add(new ExportRow(itemName, score, completed,
                        coverUrl == null || coverUrl.isBlank() ? null : coverUrl, cats));
            }
        }
        return rows;
    }

    private List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result;
    }

    private Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String csvField(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n");
        if (!needsQuotes) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private String filename(String username, String category, String ext) {
        String safe = (username == null ? "user" : username).replaceAll("[^A-Za-z0-9._-]", "_");
        return safe + "-" + category + "." + ext;
    }
}
