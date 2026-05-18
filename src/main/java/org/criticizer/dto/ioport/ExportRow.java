package org.criticizer.dto.ioport;

import org.criticizer.entity.MediaStatus;

import java.util.List;

public record ExportRow(
        String name,
        Integer score,
        boolean completed,
        MediaStatus status,
        String coverUrl,
        List<String> categories
) {}
