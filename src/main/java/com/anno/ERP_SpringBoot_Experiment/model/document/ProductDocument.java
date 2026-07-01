package com.anno.ERP_SpringBoot_Experiment.model.document;

import com.anno.ERP_SpringBoot_Experiment.model.enums.ActiveStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.util.List;

@Document(indexName = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductDocument {

    @Id
    String id;

    @Field(type = FieldType.Long)
    Long productId;

    @Field(type = FieldType.Text, analyzer = "standard")
    String name;

    @Field(type = FieldType.Keyword)
    String sku;

    @Field(type = FieldType.Keyword)
    String categoryName;

    @Field(type = FieldType.Long)
    Long categoryId;

    @Field(type = FieldType.Keyword)
    ActiveStatus status;

    @Field(type = FieldType.Nested)
    List<AttributeDocument> attributes;

    @Field(type = FieldType.Double)
    Double discountPercent;

    @Field(type = FieldType.Integer)
    Integer totalSoldQuantity;

    @Field(type = FieldType.Double)
    Double averageRating;
}
