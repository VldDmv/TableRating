package org.criticizer.dto;

import java.util.List;

/** Normalized autocomplete payload: {"results": [...]} for every category. */
public record AutocompleteResponse(List<AutocompleteResult> results) {

    public static AutocompleteResponse empty() {
        return new AutocompleteResponse(List.of());
    }
}
