package com.mentivo.repository;

import com.mentivo.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE " +
            "(c.userOne.id = :u1 AND c.userTwo.id = :u2) OR " +
            "(c.userOne.id = :u2 AND c.userTwo.id = :u1)")
    Optional<Conversation> findBetweenUsers(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT c FROM Conversation c WHERE c.userOne.id = :userId OR c.userTwo.id = :userId ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findUserConversations(@Param("userId") Long userId, Pageable pageable);
}
