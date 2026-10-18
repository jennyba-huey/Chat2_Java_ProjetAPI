package com.example.messagerie.service;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sait qui est connecte en ce moment.
 */
@Service
public class PresenceService {

    private final Set<Long> connectes = ConcurrentHashMap.newKeySet();

    public void marquerConnecte(Long utilisateurId) { connectes.add(utilisateurId); }

    public void marquerDeconnecte(Long utilisateurId) { connectes.remove(utilisateurId); }

    public boolean estConnecte(Long utilisateurId) { return connectes.contains(utilisateurId); }
}
