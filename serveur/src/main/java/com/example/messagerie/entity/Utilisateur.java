package com.example.messagerie.entity;

import jakarta.persistence.*;


@Entity
@Table(name = "utilisateurs")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    protected Utilisateur() {
        // constructeur vide exige par JPA
    }

    public Utilisateur(String username, String motDePasseHache) {
        this.username = username;
        this.motDePasse = motDePasseHache;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getMotDePasse() { return motDePasse; }
}
