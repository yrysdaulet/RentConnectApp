package com.rentconnect.repository;

import com.rentconnect.model.Listing;
import com.rentconnect.model.Review;
import com.rentconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByListing(Listing listing, Pageable pageable);
    
    Page<Review> findByUser(User user, Pageable pageable);
}

