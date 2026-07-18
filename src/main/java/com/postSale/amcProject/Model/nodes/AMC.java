package com.postSale.amcProject.Model.nodes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Node
public class AMC {
    @Id @GeneratedValue
    private String amcId;
    private LocalDate amcStartDate;
    private LocalDate amcEndDate;

    @Relationship(type = "BASED_ON", direction = Relationship.Direction.OUTGOING)
    private List<AMCOffer> amcOfferList;
}
