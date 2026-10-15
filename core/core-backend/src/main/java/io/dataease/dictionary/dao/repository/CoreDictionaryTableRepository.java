package io.dataease.dictionary.dao.repository;

import io.dataease.dictionary.dao.entity.CoreDictionaryTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface CoreDictionaryTableRepository extends JpaRepository<CoreDictionaryTable, Long> {
    Optional<CoreDictionaryTable> findByDatasourceIdAndSchemaNameAndTableName(Long datasourceId, String schemaName, String tableName);

    List<CoreDictionaryTable> findByDatasourceIdAndTableName(Long datasourceId, String tableName);

    @Query(value = """
            select t from CoreDictionaryTable t
            where t.datasourceId in :sourceIds and (
                :keyword = ''
                or locate(:keyword, lower(t.tableName)) > 0
                or locate(:keyword, lower(concat(concat(t.schemaName, '.'), t.tableName))) > 0
                or locate(:keyword, lower(t.displayName)) > 0
                or locate(:keyword, lower(t.purpose)) > 0
                or locate(:keyword, lower(t.sourceName)) > 0
                or locate(:keyword, lower(t.subjectName)) > 0
                or locate(:keyword, lower(t.remark)) > 0
                or exists (select f.id from CoreDictionaryField f where f.tableId = t.id and
                    (locate(:keyword, lower(f.fieldName)) > 0
                     or locate(:keyword, lower(f.description)) > 0))
            )
            order by case
                when :keyword = '' then 0
                when lower(t.tableName) = :keyword
                    or lower(concat(concat(t.schemaName, '.'), t.tableName)) = :keyword then 0
                when locate(:keyword, lower(t.tableName)) = 1 then 1
                when locate(:keyword, lower(t.tableName)) > 0
                    or locate(:keyword, lower(concat(concat(t.schemaName, '.'), t.tableName))) > 0 then 2
                when lower(t.displayName) = :keyword then 3
                when locate(:keyword, lower(t.displayName)) = 1 then 4
                when locate(:keyword, lower(t.displayName)) > 0 then 5
                when exists (select f.id from CoreDictionaryField f
                    where f.tableId = t.id and lower(f.fieldName) = :keyword) then 6
                when exists (select f.id from CoreDictionaryField f
                    where f.tableId = t.id and locate(:keyword, lower(f.fieldName)) = 1) then 7
                when exists (select f.id from CoreDictionaryField f
                    where f.tableId = t.id and locate(:keyword, lower(f.fieldName)) > 0) then 8
                else 9
            end, lower(t.tableName), lower(t.schemaName), t.datasourceId, t.id
            """, countQuery = """
            select count(t) from CoreDictionaryTable t
            where t.datasourceId in :sourceIds and (
                :keyword = ''
                or locate(:keyword, lower(t.tableName)) > 0
                or locate(:keyword, lower(concat(concat(t.schemaName, '.'), t.tableName))) > 0
                or locate(:keyword, lower(t.displayName)) > 0
                or locate(:keyword, lower(t.purpose)) > 0
                or locate(:keyword, lower(t.sourceName)) > 0
                or locate(:keyword, lower(t.subjectName)) > 0
                or locate(:keyword, lower(t.remark)) > 0
                or exists (select f.id from CoreDictionaryField f where f.tableId = t.id and
                    (locate(:keyword, lower(f.fieldName)) > 0
                     or locate(:keyword, lower(f.description)) > 0))
            )
            """)
    Page<CoreDictionaryTable> search(@Param("sourceIds") Collection<Long> sourceIds,
                                     @Param("keyword") String keyword, Pageable pageable);
}
