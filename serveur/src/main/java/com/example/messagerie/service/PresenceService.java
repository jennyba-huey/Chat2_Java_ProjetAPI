package com.example.messagerie.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sait qui est connecte en ce moment.
 * A l'etape 2 personne n'est connecte ; a l'etape 4, le handler WebSocket
 * appellera marquerConnecte / marquerDeconnecte a chaque connexion.
 * Ensemble thread-safe, car plusieurs connexions arrivent en parallele.
 */
@Service
public class PresenceService {

    private final Set<Long> connectes = ConcurrentHashMap.newKeySet();

    public void marquerConnecte(Long utilisateurId) { connectes.add(utilisateurId); }

    public void marquerDeconnecte(Long utilisateurId) { connectes.remove(utilisateurId); }

    public boolean estConnecte(Long utilisateurId) { return connectes.contains(utilisateurId); }
}
