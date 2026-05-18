package org.criticizer.dto.ioport;

import java.util.List;

public record ImportResult(
        int imported,
        int skipped,
        List<String> errors
) {}
