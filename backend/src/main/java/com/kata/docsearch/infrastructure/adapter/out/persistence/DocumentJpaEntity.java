package com.kata.docsearch.infrastructure.adapter.out.persistence;

import com.kata.docsearch.domain.model.DocumentFormat;
import com.kata.docsearch.domain.model.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class DocumentJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private String title;
    private String author;
    private String category;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> tags;

    private String version;
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    private DocumentFormat format;

    private String contentType;
    private long fileSize;
    private String storageKey;

    @Column(columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private String errorMessage;

    @Column(columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(columnDefinition = "timestamptz")
    private Instant updatedAt;

    /** The ID is assigned by the domain; with this Spring Data does direct INSERT instead of SELECT + INSERT. */
    @Transient
    private boolean newEntity;

    static DocumentJpaEntity newInstance() {
        var entity = new DocumentJpaEntity();
        entity.newEntity = true;
        return entity;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}