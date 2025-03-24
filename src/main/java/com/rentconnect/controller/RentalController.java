package com.rentconnect.controller;

import com.rentconnect.dto.response.MessageResponse;
import com.rentconnect.model.*;
import com.rentconnect.repository.ListingRepository;
import com.rentconnect.repository.RentalRepository;
import com.rentconnect.repository.UserRepository;
import com.rentconnect.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rentals")
public class RentalController {
    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/my-rentals")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getMyRentals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Rental> rentalsPage = rentalRepository.findByRenter(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("rentals", rentalsPage.getContent());
        response.put("currentPage", rentalsPage.getNumber());
        response.put("totalItems", rentalsPage.getTotalElements());
        response.put("totalPages", rentalsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/my-listings-rentals")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getMyListingsRentals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Rental> rentalsPage = rentalRepository.findByListing_Owner(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("rentals", rentalsPage.getContent());
        response.put("currentPage", rentalsPage.getNumber());
        response.put("totalItems", rentalsPage.getTotalElements());
        response.put("totalPages", rentalsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createRental(@RequestBody Rental rental) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<Listing> listingData = listingRepository.findById(rental.getListing().getId());
        if (!listingData.isPresent()) {
            return new ResponseEntity<>(new MessageResponse("Listing not found"), HttpStatus.NOT_FOUND);
        }
        
        Listing listing = listingData.get();
        
        // Check if the listing is available for the requested dates
        List<Rental> conflictingRentals = rentalRepository.findByListingAndStartDateGreaterThanEqualAndEndDateLessThanEqual(
                listing, rental.getStartDate(), rental.getEndDate());
        
        if (!conflictingRentals.isEmpty()) {
            return new ResponseEntity<>(new MessageResponse("Listing is not available for the requested dates"), 
                    HttpStatus.BAD_REQUEST);
        }
        
        rental.setRenter(user);
        rental.setListing(listing);
        rental.setStatus(RentalStatus.PENDING);
        
        // Calculate total price
        long days = rental.getStartDate().datesUntil(rental.getEndDate().plusDays(1)).count();
        rental.setTotalPrice(listing.getPricePerDay() * days);
        
        Rental savedRental = rentalRepository.save(rental);
        return new ResponseEntity<>(savedRental, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateRentalStatus(@PathVariable Long id, @RequestBody Map<String, String> statusUpdate) {
        Optional<Rental> rentalData = rentalRepository.findById(id);
        
        if (rentalData.isPresent()) {
            Rental rental = rentalData.get();
            
            // Check if the current user is the owner of the listing
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            if (!rental.getListing().getOwner().getId().equals(userDetails.getId())) {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to update this rental"), 
                        HttpStatus.FORBIDDEN);
            }
            
            try {
                RentalStatus newStatus = RentalStatus.valueOf(statusUpdate.get("status").toUpperCase());
                rental.setStatus(newStatus);
                
                // If the rental is completed, update the listing's rental count and earnings
                if (newStatus == RentalStatus.COMPLETED) {
                    Listing listing = rental.getListing();
                    listing.setRentalsCount(listing.getRentalsCount() + 1);
                    listing.setTotalEarnings(listing.getTotalEarnings() + rental.getTotalPrice());
                    listingRepository.save(listing);
                }
                
                return new ResponseEntity<>(rentalRepository.save(rental), HttpStatus.OK);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(new MessageResponse("Invalid status value"), HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(new MessageResponse("Rental not found"), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getRentalById(@PathVariable Long id) {
        Optional<Rental> rental = rentalRepository.findById(id);
        
        if (rental.isPresent()) {
            // Check if the current user is the renter or the owner of the listing
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            Rental rentalData = rental.get();
            if (rentalData.getRenter().getId().equals(userDetails.getId()) || 
                rentalData.getListing().getOwner().getId().equals(userDetails.getId())) {
                return new ResponseEntity<>(rentalData, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to view this rental"), 
                        HttpStatus.FORBIDDEN);
            }
        } else {
            return new ResponseEntity<>(new MessageResponse("Rental not found"), HttpStatus.NOT_FOUND);
        }
    }
}

