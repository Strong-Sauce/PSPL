//package com.postSale.amcProject.Model.Relationships;
//
//import com.postSale.amcProject.Model.nodes.Warranty;
//import org.springframework.data.neo4j.core.schema.*;
//
//@RelationshipProperties
//public class HAS_WARRANTY {
//    @Id @GeneratedValue
//    @Property
//    private String hasWarrantyId;
//
//    @TargetNode
//    private Warranty warranty;
//
//    public String getHasWarrantyId() {
//        return hasWarrantyId;
//    }
//
//    public Warranty getWarranty() {
//        return warranty;
//    }
//
//    public void setWarranty(Warranty warranty) {
//        this.warranty = warranty;
//    }
//}