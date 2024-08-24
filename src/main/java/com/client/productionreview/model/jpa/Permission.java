package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permission")
@Data
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "permissions")
    private Collection<Role> roles;

}
