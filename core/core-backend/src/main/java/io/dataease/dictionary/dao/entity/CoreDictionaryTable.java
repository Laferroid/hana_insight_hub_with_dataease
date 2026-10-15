package io.dataease.dictionary.dao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@Table(name = "core_dictionary_table", indexes = {
        @Index(name = "idx_dictionary_table_source", columnList = "datasource_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dictionary_table", columnNames = {"datasource_id", "schema_name", "table_name"})
})
@Comment("数据字典中的数据表")
public class CoreDictionaryTable {
    @Id
    @Comment("主键")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("关联的数据源 ID")
    @Column(name = "datasource_id", nullable = false)
    private Long datasourceId;

    @Comment("源数据库 schema 或 catalog")
    @Column(name = "schema_name", nullable = false, length = 255)
    private String schemaName;

    @Comment("源数据库表名")
    @Column(name = "table_name", nullable = false, length = 255)
    private String tableName;

    @Comment("数据表中文名称")
    @Column(name = "display_name", length = 1024)
    private String displayName;

    @Comment("数据表用途")
    @Column(name = "purpose", length = 4096)
    private String purpose;

    @Comment("来源系统")
    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Comment("主题节点")
    @Column(name = "subject_name", length = 255)
    private String subjectName;

    @Comment("数据表备注")
    @Column(name = "remark", length = 4096)
    private String remark;

    @Comment("字段数")
    @Column(name = "field_count", nullable = false)
    private Integer fieldCount;

    @Comment("最近导入时间戳")
    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Comment("最近导入批次 ID")
    @Column(name = "import_id", nullable = false)
    private Long importId;
}
