package com.postSale.amcProject.Model.nodes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Getter
@Setter
@Node
public class AMCOffer {
    @Id @GeneratedValue
    private String offerId;
    private String offerType; // Silver / Gold
    private Integer offerDurationMonths;
    private Double offerPrice;
    private String offerTerms;
}
