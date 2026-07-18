package com.postSale.amcProject.Model.nodes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

/**
 * User is a Neo4j graph node that stores account information.
 * @Node maps this class to a (:User) node in the Neo4j database.
 */
@Getter
@Setter
@Node
public class User {

    @Id
    @GeneratedValue
    private String id;

    private String name;

    private String email; // Used as the login username

    private String password; // Always stored as BCrypt hash, never plain text

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Password reset fields - null when no reset is pending
    private String resetToken;        // Random token sent in the reset email link

    private LocalDateTime resetTokenExpiresAt; // When the token stops being valid
}

