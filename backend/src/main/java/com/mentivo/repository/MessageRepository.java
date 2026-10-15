package com.mentivo.repository;

import com.mentivo.entity.Message;
import com.mentivo.entity.enums.ReadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    long countByRecipientIdAndReadStatus(Long recipientId, ReadStatus readStatus);

    long countByConversationIdAndRecipientIdAndReadStatus(Long conversationId, Long recipientId, ReadStatus readStatus);

    @Modifying
    @Query("UPDATE Message m SET m.readStatus = 'READ' WHERE m.conversation.id = :convId AND m.recipient.id = :recipientId AND m.readStatus = 'UNREAD'")
    void markMessagesAsRead(@Param("convId") Long convId, @Param("recipientId") Long recipientId);
}
