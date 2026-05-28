package org.criticizer.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * Junction entity that defines which genres are applicable to which media types. Uses composite
 * primary key (genre_id, media_type).
 */
@Entity
@Table(name = "genre_applicability")
@IdClass(GenreApplicability.GenreApplicabilityId.class)
public class GenreApplicability {

    @Id
    @Column(name = "genre_id", nullable = false)
    private Integer genreId;

    @Id
    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    // Constructors

    protected GenreApplicability() {}

    public GenreApplicability(Integer genreId, String mediaType) {
        this.genreId = genreId;
        this.mediaType = mediaType;
    }

    // Getters and Setters

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenreApplicability that = (GenreApplicability) o;
        return Objects.equals(genreId, that.genreId) && Objects.equals(mediaType, that.mediaType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(genreId, mediaType);
    }

    // ============= Composite Key Class =============

    /** Composite primary key for GenreApplicability. Required for @IdClass annotation. */
    public static class GenreApplicabilityId implements Serializable {

        private Integer genreId;
        private String mediaType;

        public GenreApplicabilityId() {}

        public GenreApplicabilityId(Integer genreId, String mediaType) {
            this.genreId = genreId;
            this.mediaType = mediaType;
        }

        // Getters and Setters
        public Integer getGenreId() {
            return genreId;
        }

        public void setGenreId(Integer genreId) {
            this.genreId = genreId;
        }

        public String getMediaType() {
            return mediaType;
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GenreApplicabilityId that = (GenreApplicabilityId) o;
            return Objects.equals(genreId, that.genreId)
                    && Objects.equals(mediaType, that.mediaType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(genreId, mediaType);
        }
    }
}
