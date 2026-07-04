package org.criticizer.dto;

/** One normalized autocomplete suggestion, independent of the upstream API. */
public record AutocompleteResult(
        String name, Integer year, String rating, String imageUrl, String coverUrl) {}
