package com.rentconnect.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "listings")
@EntityListeners(AuditingEntityListener.class)
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "price_per_day")
    private Double pricePerDay;

    @Column(name = "security_deposit")
    private Double securityDeposit;

    private String location;

    private String category;

    @Column(name = "price_unit")
    private String priceUnit;

    private Double rating;

    @Column(name = "reviews_count")
    private Integer reviewsCount;

    @Column(unique = true)
    private String slug;

    @ElementCollection
    @CollectionTable(name = "listing_features", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "feature")
    private List<String> features = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "listing_images", joinColumns = @JoinColumn(name = "listing_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "rentals_count")
    private Integer rentalsCount;

    @Column(name = "total_earnings")
    private Double totalEarnings;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

