package com.rentconnect.controller;

import com.rentconnect.dto.response.MessageResponse;
import com.rentconnect.model.User;
import com.rentconnect.repository.UserRepository;
import com.rentconnect.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        Optional<User> user = userRepository.findById(userDetails.getId());
        if (user.isPresent()) {
            return new ResponseEntity<>(user.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateCurrentUser(@RequestBody User userDetails) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl currentUserDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        Optional<User> userData = userRepository.findById(userDetails.getId());
        if (userData.isPresent()) {
            User user = userData.get();
            
            // Update user fields
            user.setFirstName(currentUserDetails.getFirstName());
            user.setLastName(currentUserDetails.getLastName());
            user.setPicture(currentUserDetails.getPicture());
            
            return new ResponseEntity<>(userRepository.save(user), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return new ResponseEntity<>(user.get(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);
        }
    }
}

