package io.dataease.dictionary.dao.repository;

import io.dataease.dictionary.dao.entity.CoreDictionaryField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CoreDictionaryFieldRepository extends JpaRepository<CoreDictionaryField, Long> {
    List<CoreDictionaryField> findByTableIdOrderByOrdinal(Long tableId);

    List<CoreDictionaryField> findByTableIdInOrderByTableIdAscOrdinalAsc(Collection<Long> tableIds);

    @Modifying
    @Query("delete from CoreDictionaryField f where f.tableId = :tableId")
    void deleteByTableId(@Param("tableId") Long tableId);
}
