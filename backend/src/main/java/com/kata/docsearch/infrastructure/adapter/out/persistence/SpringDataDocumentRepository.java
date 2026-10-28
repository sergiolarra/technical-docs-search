package com.kata.docsearch.infrastructure.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataDocumentRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentJpaEntity d where d.id = :id")
    Optional<DocumentJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}