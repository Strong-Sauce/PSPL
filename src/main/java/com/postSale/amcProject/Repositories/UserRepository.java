package com.postSale.amcProject.Repositories;

import com.postSale.amcProject.Model.nodes.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository handles all database operations for the User node.
 * Spring Data Neo4j auto-generates the Cypher queries for us.
 */
@Repository
public interface UserRepository extends Neo4jRepository<User, String> {

    // Find a user by their email address (used during login)
    Optional<User> findByEmail(String email);

    // Check if an email is already registered (used during signup to prevent duplicates)
    boolean existsByEmail(String email);

    // Find a user by their password reset token (used during password reset)
    Optional<User> findByResetToken(String resetToken);
}

