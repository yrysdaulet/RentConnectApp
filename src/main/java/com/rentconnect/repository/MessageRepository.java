package com.rentconnect.repository;

import com.rentconnect.model.Message;
import com.rentconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE m.sender = :user OR m.recipient = :user ORDER BY m.createdAt DESC")
    Page<Message> findUserMessages(User user, Pageable pageable);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipient = :user AND m.isRead = false")
    Long countUnreadMessages(User user);
}

