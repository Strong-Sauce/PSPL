package com.postSale.amcProject.Repositories;

import com.postSale.amcProject.Model.nodes.Sale;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends Neo4jRepository<Sale, String> {

    @Query("""
    MATCH (c:Customer)-[:PURCHASED]->(s:Sale)
    WHERE c.custId = $customerId
    RETURN s
    """)
    List<Sale> findAllSalesByCustomerId(String customerId);
}
