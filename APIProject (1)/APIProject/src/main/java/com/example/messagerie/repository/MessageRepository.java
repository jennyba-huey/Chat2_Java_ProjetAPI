package com.example.messagerie.repository;

import com.example.messagerie.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Tous les messages entre A et B, dans les deux sens, du plus ancien au plus recent. */
    @Query("""
           SELECT m FROM Message m
           WHERE (m.expediteur.id = :a AND m.destinataire.id = :b)
              OR (m.expediteur.id = :b AND m.destinataire.id = :a)
           ORDER BY m.dateEnvoi ASC, m.id ASC
           """)
    List<Message> findConversation(@Param("a") Long a, @Param("b") Long b);
}
