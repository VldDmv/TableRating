package org.criticizer.entity;

/**
 * Tracks where a media item sits in the user's personal pipeline.
 * Orthogonal to {@code completed} — wishlist/backlog/dropped describe intent,
 * completed describes whether the user finished it.
 */
public enum MediaStatus {
    NONE,
    WISHLIST,
    BACKLOG,
    DROPPED
}
