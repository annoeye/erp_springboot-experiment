package com.anno.ERP_SpringBoot_Experiment.model.document;

import com.anno.ERP_SpringBoot_Experiment.model.enums.StockStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttributeDocument {

    @Field(type = FieldType.Long)
    Long attributeId;

    @Field(type = FieldType.Text, analyzer = "standard")
    String name;

    @Field(type = FieldType.Keyword)
    String sku;

    @Field(type = FieldType.Double)
    Double price;

    @Field(type = FieldType.Double)
    Double salePrice;

    @Field(type = FieldType.Keyword)
    StockStatus statusProduct;

    @Field(type = FieldType.Keyword)
    Set<String> keywords;
}
