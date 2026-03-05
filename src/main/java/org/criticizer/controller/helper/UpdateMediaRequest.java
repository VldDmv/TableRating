package org.criticizer.controller.helper;

import java.util.List;

/**
 * Common interface for all media update requests.
 * Provides unified contract for Update operations.
 */
public interface UpdateMediaRequest {
    String name();
    String coverUrl();
    Integer score();
    List<Integer> categoryIds();
}