package com.postSale.amcProject.Repositories;

import com.postSale.amcProject.Model.nodes.Warranty;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarrantyRepository extends Neo4jRepository<Warranty, String> {

    @Query("""
    MATCH (w:Warranty)
    WHERE w.warrantyEndDate <= date() + duration('P30D')
    RETURN w
    """)
    List<Warranty> findWarrantiesExpiringSoon();

    @Query("""
    MATCH (c:Customer)-[:PURCHASED]->(s:Sale)-[:OF_PRODUCT]->(p:Product)-[:HAS_WARRANTY]->(w:Warranty)
    WHERE c.custId = $custId
    RETURN w
    """)
    List<Warranty> findWarrantiesByCustomerId(String custId);

}

