package com.mentivo.repository;

import com.mentivo.entity.Connection;
import com.mentivo.entity.enums.ConnectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    @Query("SELECT c FROM Connection c WHERE (c.sender.id = :u1 AND c.receiver.id = :u2) OR (c.sender.id = :u2 AND c.receiver.id = :u1)")
    Optional<Connection> findConnectionBetween(@Param("u1") Long u1, @Param("u2") Long u2);

    @Query("SELECT c FROM Connection c WHERE (c.sender.id = :userId OR c.receiver.id = :userId) AND c.status = 'ACCEPTED'")
    Page<Connection> findActiveConnectionsForUser(@Param("userId") Long userId, Pageable pageable);

    Page<Connection> findByReceiverIdAndStatus(Long receiverId, ConnectionStatus status, Pageable pageable);

    Page<Connection> findBySenderIdAndStatus(Long senderId, ConnectionStatus status, Pageable pageable);

    long countByReceiverIdAndStatus(Long receiverId, ConnectionStatus status);
}
