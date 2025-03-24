package com.rentconnect.repository;

import com.rentconnect.model.Listing;
import com.rentconnect.model.Rental;
import com.rentconnect.model.RentalStatus;
import com.rentconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RentalRepository extends JpaRepository<Rental, Long> {
    Page<Rental> findByRenter(User renter, Pageable pageable);
    
    Page<Rental> findByListing_Owner(User owner, Pageable pageable);
    
    List<Rental> findByListingAndStatus(Listing listing, RentalStatus status);
    
    List<Rental> findByListingAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
            Listing listing, LocalDate startDate, LocalDate endDate);
}

