package com.postSale.amcProject.Model.nodes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Getter
@Setter
@Node
public class Product {
    @Id @GeneratedValue
    private String productId;
    private String productName;
    private String productSerialNumber;

    @Relationship(type = "HAS_WARRANTY", direction = Relationship.Direction.OUTGOING)
    private List<Warranty> warrantyList;
}
