package com.rentconnect.repository;

import com.rentconnect.model.Listing;
import com.rentconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {
    Optional<Listing> findBySlug(String slug);
    
    Page<Listing> findByIsActiveTrue(Pageable pageable);
    
    Page<Listing> findByOwner(User owner, Pageable pageable);
    
    Page<Listing> findByCategoryAndIsActiveTrue(String category, Pageable pageable);
    
    @Query("SELECT l FROM Listing l WHERE l.isActive = true AND " +
           "(LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Listing> search(String query, Pageable pageable);
    
    @Query("SELECT l FROM Listing l WHERE l.isActive = true AND " +
           "l.category = :category AND " +
           "l.pricePerDay BETWEEN :minPrice AND :maxPrice")
    Page<Listing> findByCategoryAndPriceRange(String category, Double minPrice, Double maxPrice, Pageable pageable);
    
    List<Listing> findTop8ByIsActiveTrueOrderByCreatedAtDesc();
}

