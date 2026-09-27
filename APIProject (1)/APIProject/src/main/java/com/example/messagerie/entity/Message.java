package com.example.messagerie.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "messages",
       indexes = @Index(name = "idx_conversation", columnList = "expediteur_id, destinataire_id"))
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private Utilisateur expediteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    protected Message() {
    }

    public Message(Utilisateur expediteur, Utilisateur destinataire, String contenu) {
        this.expediteur = expediteur;
        this.destinataire = destinataire;
        this.contenu = contenu;
    }

    /** Horodatage serveur, arrondi a la seconde (format 2026-09-21T10:15:30). */
    @PrePersist
    void horodater() {
        this.dateEnvoi = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() { return id; }
    public Utilisateur getExpediteur() { return expediteur; }
    public Utilisateur getDestinataire() { return destinataire; }
    public String getContenu() { return contenu; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
}
