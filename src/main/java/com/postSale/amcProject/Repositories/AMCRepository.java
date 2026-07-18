package com.postSale.amcProject.Repositories;

import com.postSale.amcProject.Model.nodes.AMC;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AMCRepository extends Neo4jRepository<AMC, String> {
}
