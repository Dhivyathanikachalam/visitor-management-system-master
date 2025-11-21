package com.java.vms.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.java.vms.model.VisitStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Visit {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VisitStatus visitStatus;

    @Column
    private LocalDateTime inTime;

    @Column
    private LocalDateTime outTime;

    @Column(nullable = false)
    private String visitorImgUrl;

    @Column(nullable = false)
    private String purpose;

    @Column(nullable = false)
    private Integer numOfGuests;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "visitor_id", nullable = false)
    /* Ignore proxy metadata - To address the issue in serializing lazy hibernated proxy (Address) object */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Visitor visitor;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;
}
