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
@Table(name = "core_dictionary_field", indexes = {
        @Index(name = "idx_dictionary_field_table", columnList = "table_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_dictionary_field", columnNames = {"table_id", "field_name"})
})
@Comment("数据字典中的数据字段")
public class CoreDictionaryField {
    @Id
    @Comment("主键")
    @Column(name = "id", nullable = false)
    private Long id;

    @Comment("数据字典表 ID")
    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Comment("源字段名")
    @Column(name = "field_name", nullable = false, length = 255)
    private String fieldName;

    @Comment("源字段数据类型")
    @Column(name = "data_type", length = 255)
    private String dataType;

    @Comment("字段说明")
    @Column(name = "description", length = 4096)
    private String description;

    @Comment("是否主键")
    @Column(name = "primary_key")
    private Boolean primaryKey;

    @Comment("是否必填")
    @Column(name = "required_field")
    private Boolean requiredField;

    @Comment("字段备注")
    @Column(name = "remark", length = 4096)
    private String remark;

    @Comment("字段展示顺序")
    @Column(name = "ordinal", nullable = false)
    private Integer ordinal;
}
