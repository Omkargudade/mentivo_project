package com.mentivo.repository;

import com.mentivo.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b WHERE " +
            "(b.blocker.id = :u1 AND b.blocked.id = :u2) OR " +
            "(b.blocker.id = :u2 AND b.blocked.id = :u1)")
    boolean isBlockedBetween(@Param("u1") Long u1, @Param("u2") Long u2);
}
