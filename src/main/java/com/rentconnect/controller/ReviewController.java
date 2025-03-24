package com.rentconnect.controller;

import com.rentconnect.dto.response.MessageResponse;
import com.rentconnect.model.Listing;
import com.rentconnect.model.Review;
import com.rentconnect.model.User;
import com.rentconnect.repository.ListingRepository;
import com.rentconnect.repository.ReviewRepository;
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
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<Map<String, Object>> getReviewsByListing(
            @PathVariable Long listingId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Optional<Listing> listingData = listingRepository.findById(listingId);
        if (!listingData.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        Listing listing = listingData.get();
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Review> reviewsPage = reviewRepository.findByListing(listing, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("reviews", reviewsPage.getContent());
        response.put("currentPage", reviewsPage.getNumber());
        response.put("totalItems", reviewsPage.getTotalElements());
        response.put("totalPages", reviewsPage.getTotalPages());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createReview(@RequestBody Review review) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<Listing> listingData = listingRepository.findById(review.getListing().getId());
        if (!listingData.isPresent()) {
            return new ResponseEntity<>(new MessageResponse("Listing not found"), HttpStatus.NOT_FOUND);
        }
        
        Listing listing = listingData.get();
        
        review.setUser(user);
        review.setListing(listing);
        
        Review savedReview = reviewRepository.save(review);
        
        // Update listing rating
        int reviewsCount = listing.getReviewsCount() + 1;
        double newRating = ((listing.getRating() * listing.getReviewsCount()) + review.getRating()) / reviewsCount;
        
        listing.setReviewsCount(reviewsCount);
        listing.setRating(newRating);
        listingRepository.save(listing);
        
        // Update owner rating
        User owner = listing.getOwner();
        int ownerReviewsCount = owner.getReviewsCount() + 1;
        double ownerNewRating = ((owner.getRating() * owner.getReviewsCount()) + review.getRating()) / ownerReviewsCount;
        
        owner.setReviewsCount(ownerReviewsCount);
        owner.setRating(ownerNewRating);
        userRepository.save(owner);
        
        return new ResponseEntity<>(savedReview, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        Optional<Review> reviewData = reviewRepository.findById(id);
        
        if (reviewData.isPresent()) {
            Review review = reviewData.get();
            
            // Check if the current user is the author of the review
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            if (!review.getUser().getId().equals(userDetails.getId())) {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to delete this review"), 
                        HttpStatus.FORBIDDEN);
            }
            
            // Update listing rating
            Listing listing = review.getListing();
            int reviewsCount = listing.getReviewsCount() - 1;
            
            if (reviewsCount > 0) {
                double newRating = ((listing.getRating() * listing.getReviewsCount()) - review.getRating()) / reviewsCount;
                listing.setRating(newRating);
            } else {
                listing.setRating(0.0);
            }
            
            listing.setReviewsCount(reviewsCount);
            listingRepository.save(listing);
            
            // Update owner rating
            User owner = listing.getOwner();
            int ownerReviewsCount = owner.getReviewsCount() - 1;
            
            if (ownerReviewsCount > 0) {
                double ownerNewRating = ((owner.getRating() * owner.getReviewsCount()) - review.getRating()) / ownerReviewsCount;
                owner.setRating(ownerNewRating);
            } else {
                owner.setRating(0.0);
            }
            
            owner.setReviewsCount(ownerReviewsCount);
            userRepository.save(owner);
            
            reviewRepository.delete(review);
            return new ResponseEntity<>(new MessageResponse("Review deleted successfully"), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("Review not found"), HttpStatus.NOT_FOUND);
        }
    }
}

