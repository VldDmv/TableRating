package org.criticizer.entity;

/**
 * Lifecycle status of a media item. Replaces the old boolean "completed" flag (migration V3 maps
 * true → COMPLETED, false → PLANNED).
 */
public enum MediaStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    DROPPED;

    /** The status the toggle button advances to (cycles through all four). */
    public MediaStatus next() {
        MediaStatus[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
