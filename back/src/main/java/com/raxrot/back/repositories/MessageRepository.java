package com.raxrot.back.repositories;

import com.raxrot.back.models.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
        select m from Message m
        where (m.sender.userId = :me and m.recipient.userId = :peer)
           or (m.sender.userId = :peer and m.recipient.userId = :me)
        order by m.createdAt asc, m.id asc
    """)
    Page<Message> findConversation(@Param("me") Long meId,
                                   @Param("peer") Long peerId,
                                   Pageable p);

    @Query("""
        select m from Message m
        where ((m.sender.userId = :me and m.recipient.userId = :peer)
            or (m.sender.userId = :peer and m.recipient.userId = :me))
          and m.id > :afterId
        order by m.createdAt asc, m.id asc
    """)
    List<Message> findNewAfter(@Param("me") Long meId,
                               @Param("peer") Long peerId,
                               @Param("afterId") Long afterId);

    Page<Message> findBySender_UserIdOrRecipient_UserIdOrderByCreatedAtDesc(
            Long senderId, Long recipientId, Pageable pageable);

    @Modifying
    @Query("""
        delete from Message m
        where m.sender.userId = :id or m.recipient.userId = :id
    """)
    void deleteAllForUser(@Param("id") Long userId);
}