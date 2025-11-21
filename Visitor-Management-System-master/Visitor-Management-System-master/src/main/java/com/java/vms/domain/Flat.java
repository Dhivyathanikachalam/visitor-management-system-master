package com.java.vms.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.java.vms.model.FlatStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Set;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flat {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String flatNum;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private FlatStatus flatStatus;

    @OneToMany(mappedBy = "flat", fetch = FetchType.LAZY)
    @JsonBackReference
    private Set<User> users;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}
