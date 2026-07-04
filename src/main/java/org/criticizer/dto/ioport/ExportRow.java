package org.criticizer.dto.ioport;

import java.util.List;

public record ExportRow(
        String name, Integer score, String status, String coverUrl, List<String> categories) {}
