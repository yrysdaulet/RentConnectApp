package com.rentconnect.controller;

import com.rentconnect.dto.response.MessageResponse;
import com.rentconnect.model.Listing;
import com.rentconnect.model.User;
import com.rentconnect.repository.ListingRepository;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/listings")
public class ListingController {
    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Listing> listingsPage = listingRepository.findByIsActiveTrue(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("listings", listingsPage.getContent());
        response.put("currentPage", listingsPage.getNumber());
        response.put("totalItems", listingsPage.getTotalElements());
        response.put("totalPages", listingsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Listing>> getFeaturedListings() {
        List<Listing> featuredListings = listingRepository.findTop8ByIsActiveTrueOrderByCreatedAtDesc();
        return new ResponseEntity<>(featuredListings, HttpStatus.OK);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getListingsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Listing> listingsPage = listingRepository.findByCategoryAndIsActiveTrue(category, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("listings", listingsPage.getContent());
        response.put("currentPage", listingsPage.getNumber());
        response.put("totalItems", listingsPage.getTotalElements());
        response.put("totalPages", listingsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchListings(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Listing> listingsPage = listingRepository.search(query, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("listings", listingsPage.getContent());
        response.put("currentPage", listingsPage.getNumber());
        response.put("totalItems", listingsPage.getTotalElements());
        response.put("totalPages", listingsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getListingBySlug(@PathVariable String slug) {
        Optional<Listing> listing = listingRepository.findBySlug(slug);
        if (listing.isPresent()) {
            return new ResponseEntity<>(listing.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("Listing not found"), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createListing(@RequestBody Listing listing) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        listing.setOwner(user);
        listing.setIsActive(true);
        listing.setRating(0.0);
        listing.setReviewsCount(0);
        listing.setRentalsCount(0);
        listing.setTotalEarnings(0.0);
        
        // Generate slug from title
        String slug = listing.getTitle().toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");
        
        // Check if slug exists and append a number if needed
        String baseSlug = slug;
        int counter = 1;
        while (listingRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        
        listing.setSlug(slug);
        
        Listing savedListing = listingRepository.save(listing);
        return new ResponseEntity<>(savedListing, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateListing(@PathVariable Long id, @RequestBody Listing listingDetails) {
        Optional<Listing> listingData = listingRepository.findById(id);
        
        if (listingData.isPresent()) {
            Listing listing = listingData.get();
            
            // Check if the current user is the owner
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            if (!listing.getOwner().getId().equals(userDetails.getId())) {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to update this listing"), 
                        HttpStatus.FORBIDDEN);
            }
            
            // Update listing fields
            listing.setTitle(listingDetails.getTitle());
            listing.setDescription(listingDetails.getDescription());
            listing.setPricePerDay(listingDetails.getPricePerDay());
            listing.setSecurityDeposit(listingDetails.getSecurityDeposit());
            listing.setLocation(listingDetails.getLocation());
            listing.setCategory(listingDetails.getCategory());
            listing.setFeatures(listingDetails.getFeatures());
            listing.setImages(listingDetails.getImages());
            
            return new ResponseEntity<>(listingRepository.save(listing), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("Listing not found"), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteListing(@PathVariable Long id) {
        Optional<Listing> listingData = listingRepository.findById(id);
        
        if (listingData.isPresent()) {
            Listing listing = listingData.get();
            
            // Check if the current user is the owner
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            if (!listing.getOwner().getId().equals(userDetails.getId())) {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to delete this listing"), 
                        HttpStatus.FORBIDDEN);
            }
            
            listingRepository.delete(listing);
            return new ResponseEntity<>(new MessageResponse("Listing deleted successfully"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("Listing not found"), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Listing> listingsPage = listingRepository.findByOwner(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("listings", listingsPage.getContent());
        response.put("currentPage", listingsPage.getNumber());
        response.put("totalItems", listingsPage.getTotalElements());
        response.put("totalPages", listingsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

