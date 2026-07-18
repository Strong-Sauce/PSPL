package com.postSale.amcProject.Repositories;

import com.postSale.amcProject.Model.nodes.Customer;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends Neo4jRepository<Customer, String> {

}
