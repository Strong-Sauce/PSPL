package com.postSale.amcProject.Repositories;

import com.postSale.amcProject.Model.nodes.AMCOffer;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AMCOfferRepository extends Neo4jRepository<AMCOffer, String> {
}