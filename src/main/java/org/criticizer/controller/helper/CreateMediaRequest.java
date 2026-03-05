package org.criticizer.controller.helper;

import java.util.List;

/**
 * Common interface for all media creation requests.
 * Provides unified contract for Create operations.
 *
 * NOT sealed - allows implementations in any package.
 */
public interface CreateMediaRequest {
    String name();
    String coverUrl();
    Integer score();
    List<Integer> categoryIds();
}
