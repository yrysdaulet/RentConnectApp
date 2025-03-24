package com.rentconnect.controller;

import com.rentconnect.dto.response.MessageResponse;
import com.rentconnect.model.Message;
import com.rentconnect.model.User;
import com.rentconnect.repository.MessageRepository;
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
@RequestMapping("/api/messages")
public class MessageController {
    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getUserMessages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        
        Page<Message> messagesPage = messageRepository.findUserMessages(user, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("messages", messagesPage.getContent());
        response.put("currentPage", messagesPage.getNumber());
        response.put("totalItems", messagesPage.getTotalElements());
        response.put("totalPages", messagesPage.getTotalPages());
        response.put("unreadCount", messageRepository.countUnreadMessages(user));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        User sender = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<User> recipientData = userRepository.findById(message.getRecipient().getId());
        if (!recipientData.isPresent()) {
            return new ResponseEntity<>(new MessageResponse("Recipient not found"), HttpStatus.NOT_FOUND);
        }
        
        message.setSender(sender);
        message.setIsRead(false);
        
        Message savedMessage = messageRepository.save(message);
        return new ResponseEntity<>(savedMessage, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getMessageById(@PathVariable Long id) {
        Optional<Message> messageData = messageRepository.findById(id);
        
        if (messageData.isPresent()) {
            Message message = messageData.get();
            
            // Check if the current user is the sender or recipient
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            if (message.getSender().getId().equals(userDetails.getId()) || 
                message.getRecipient().getId().equals(userDetails.getId())) {
                
                // Mark as read if the current user is the recipient
                if (message.getRecipient().getId().equals(userDetails.getId()) && !message.getIsRead()) {
                    message.setIsRead(true);
                    messageRepository.save(message);
                }
                
                return new ResponseEntity<>(message, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to view this message"), 
                        HttpStatus.FORBIDDEN);
            }
        } else {
            return new ResponseEntity<>(new MessageResponse("Message not found"), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteMessage(@PathVariable Long id) {
        Optional<Message> messageData = messageRepository.findById(id);
        
        if (messageData.isPresent()) {
            Message message = messageData.get();
            
            // Check if the current user is the sender or recipient
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            if (message.getSender().getId().equals(userDetails.getId()) || 
                message.getRecipient().getId().equals(userDetails.getId())) {
                
                messageRepository.delete(message);
                return new ResponseEntity<>(new MessageResponse("Message deleted successfully"), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new MessageResponse("You are not authorized to delete this message"), 
                        HttpStatus.FORBIDDEN);
            }
        } else {
            return new ResponseEntity<>(new MessageResponse("Message not found"), HttpStatus.NOT_FOUND);
        }
    }
}

