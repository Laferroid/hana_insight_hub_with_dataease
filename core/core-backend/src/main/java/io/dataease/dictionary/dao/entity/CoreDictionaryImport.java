package io.dataease.dictionary.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(name = "core_dictionary_import")
@Comment("数据字典导入记录")
public class CoreDictionaryImport {
    @Id
    @Comment("导入批次 ID")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("关联的数据源 ID")
    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Comment("上传的文件名")
    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Comment("导入表数")
    @Column(name = "table_count", nullable = false)
    private Integer tableCount;

    @Comment("导入字段数")
    @Column(name = "field_count", nullable = false)
    private Integer fieldCount;

    @Comment("导入人 ID")
    @Column(name = "imported_by", nullable = false)
    private Long importedBy;

    @Comment("导入时间戳")
    @Column(name = "imported_at", nullable = false)
    private Long importedAt;
}
