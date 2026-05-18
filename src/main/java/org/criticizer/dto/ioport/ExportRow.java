package org.criticizer.dto.ioport;

import java.util.List;

public record ExportRow(
        String name,
        Integer score,
        boolean completed,
        String coverUrl,
        List<String> categories
) {}
