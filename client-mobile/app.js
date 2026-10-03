"use strict";

/* =========================================================
   CONFIGURATION
   Meme logique que le client bureau : on reprend l'adresse de la page.
   En local c'est "localhost" ; le jour de la demo, si la page est ouverte
   via http://192.168.x.x:5500, le client ira chercher le serveur sur 192.168.x.x.
   Si la page est servie par une autre machine que le serveur, remplacer par
   l'IP du PC serveur, par exemple : const SERVEUR = "192.168.1.20:8080";
   ========================================================= */
const SERVEUR = `${window.location.hostname || "localhost"}:8080`;

const URL_API = `http://${SERVEUR}`;
const URL_WS = `ws://${SERVEUR}/ws/messages`;

const CLE_SESSION = "messagerie.mobile.session";
const DELAI_MAX_RECONNEXION_MS = 10000;   // attente maximum entre deux tentatives
const DELAI_SIGNAL_ECRITURE_MS = 2000;    // au plus un signal "en train d'ecrire" toutes les 2 s
const DUREE_INDICATEUR_ECRITURE_MS = 3000; // l'indicateur s'efface apres 3 s sans nouvelle saisie

/* Etat de l'application */
const etat = {
  session: null,              // { token, id, username }
  modeInscription: false,
  utilisateurs: [],           // [{ id, username, connecte, nonLus }]
  contactId: null,            // id de la conversation ouverte
  messages: [],               // messages de la conversation ouverte, tries par id
  ecritureDe: null,           // id de l'utilisateur en train d'ecrire
  minuteurEcriture: null,
  dernierSignalEcriture: 0,
  socket: null,
  canalOuvert: false,
  canalActif: false,          // false apres deconnexion : plus de reconnexion
  tentative: 0,
  minuteurReconnexion: null,
  minuteurToast: null,
};

const $ = (id) => document.getElementById(id);
const son = new Audio("notification.mp3");


/* =========================================================
   SESSION (sessionStorage : propre a l'onglet, comme le client bureau)
   ========================================================= */

function lireSession() {
  try { return JSON.parse(sessionStorage.getItem(CLE_SESSION)); } catch { return null; }
}

function ecrireSession(session) {
  try { sessionStorage.setItem(CLE_SESSION, JSON.stringify(session)); } catch { /* ignore */ }
}

function effacerSession() {
  try { sessionStorage.removeItem(CLE_SESSION); } catch { /* ignore */ }
}


/* =========================================================
   OUTILS D'AFFICHAGE
   ========================================================= */

function afficherEcran(nom) {
  ["connexion", "liste", "conversation"].forEach((e) => {
    $(`ecran-${e}`).hidden = e !== nom;
  });
  window.scrollTo(0, 0);
}

function afficherErreur(idZone, texte) {
  const zone = $(idZone);
  zone.textContent = texte || "";
  zone.hidden = !texte;
}

function afficherToast(texte) {
  const toast = $("toast");
  toast.textContent = texte;
  toast.hidden = false;
  clearTimeout(etat.minuteurToast);
  etat.minuteurToast = setTimeout(() => { toast.hidden = true; }, 3500);
}

function jouerSon() {
  son.currentTime = 0;
  son.play().catch(() => {});
}

/** "14:05" si le message date d'aujourd'hui, "27/09 14:05" sinon. */
function formaterHeure(dateIso) {
  const d = new Date(dateIso);
  if (Number.isNaN(d.getTime())) return "";
  const heure = d.toLocaleTimeString("fr-FR", { hour: "2-digit", minute: "2-digit" });
  if (d.toDateString() === new Date().toDateString()) return heure;
  return `${d.toLocaleDateString("fr-FR", { day: "2-digit", month: "2-digit" })} ${heure}`;
}

function initiale(username) {
  return (username || "?").charAt(0).toUpperCase();
}


/* =========================================================
   APPELS A L'API REST
   ========================================================= */

/**
 * Appel REST generique : ajoute le jeton, lit le JSON et transforme les
 * erreurs du serveur ({status, erreur, message, date}) en exception.
 */
async function appeler(chemin, { methode = "GET", corps } = {}) {
  const entetes = { "Content-Type": "application/json" };
  if (etat.session?.token) entetes.Authorization = `Bearer ${etat.session.token}`;

  let reponse;
  try {
    reponse = await fetch(`${URL_API}${chemin}`, {
      method: methode,
      headers: entetes,
      body: corps ? JSON.stringify(corps) : undefined,
    });
  } catch {
    throw new Error(`Serveur injoignable (${SERVEUR}). Vérifie qu'il est lancé et que tu es sur le même réseau Wi-Fi.`);
  }

  const donnees = await reponse.json().catch(() => null);

  if (!reponse.ok) {
    const erreur = new Error(donnees?.message || `Erreur ${reponse.status}`);
    erreur.status = reponse.status;
    throw erreur;
  }
  return donnees;
}

/** Jeton expire ou invalide sur un appel protege : retour a l'ecran de connexion. */
function estSessionExpiree(erreur) {
  if (erreur.status === 401 && etat.session) {
    deconnecter("Ta session a expiré. Reconnecte-toi.");
    return true;
  }
  return false;
}


/* =========================================================
   ECRAN 1 : CONNEXION / CREATION DE COMPTE
   ========================================================= */

function majModeConnexion() {
  const inscription = etat.modeInscription;
  $("titre-connexion").textContent = inscription ? "Créer un compte" : "Connexion";
  $("btn-valider").textContent = inscription ? "S'inscrire" : "Se connecter";
  $("btn-changer-mode").textContent = inscription
    ? "Déjà un compte ? Se connecter"
    : "Pas de compte ? Créer un compte";
  $("champ-mdp").autocomplete = inscription ? "new-password" : "current-password";
}

$("btn-changer-mode").addEventListener("click", () => {
  etat.modeInscription = !etat.modeInscription;
  afficherErreur("erreur-connexion", null);
  majModeConnexion();
});

$("form-connexion").addEventListener("submit", async (evenement) => {
  evenement.preventDefault();
  const username = $("champ-username").value.trim();
  const motDePasse = $("champ-mdp").value;

  // Memes regles de saisie que le client bureau
  if (!username || !motDePasse) {
    afficherErreur("erreur-connexion", "Saisis un nom d'utilisateur et un mot de passe.");
    return;
  }
  if (etat.modeInscription && username.length < 3) {
    afficherErreur("erreur-connexion", "Le nom d'utilisateur doit contenir au moins 3 caractères.");
    return;
  }
  if (etat.modeInscription && motDePasse.length < 6) {
    afficherErreur("erreur-connexion", "Le mot de passe doit contenir au moins 6 caractères.");
    return;
  }

  afficherErreur("erreur-connexion", null);
  const bouton = $("btn-valider");
  bouton.disabled = true;
  bouton.textContent = "Patiente...";

  try {
    if (etat.modeInscription) {
      await appeler("/api/comptes", { methode: "POST", corps: { username, motDePasse } });
    }
    const reponse = await appeler("/api/auth/login", { methode: "POST", corps: { username, motDePasse } });
    demarrerSession({ token: reponse.token, id: reponse.id, username: reponse.username });
  } catch (erreur) {
    afficherErreur("erreur-connexion", erreur.message);
  } finally {
    bouton.disabled = false;
    majModeConnexion();
  }
});

function demarrerSession(session) {
  etat.session = session;
  ecrireSession(session);

  $("mon-nom").textContent = session.username;
  $("champ-mdp").value = "";
  etat.modeInscription = false;
  majModeConnexion();

  afficherEcran("liste");
  chargerUtilisateurs();
  ouvrirCanal();
}

function deconnecter(message) {
  fermerCanal();
  effacerSession();

  etat.session = null;
  etat.utilisateurs = [];
  etat.contactId = null;
  etat.messages = [];
  etat.ecritureDe = null;
  clearTimeout(etat.minuteurEcriture);

  $("liste-utilisateurs").replaceChildren();
  $("liste-messages").replaceChildren();
  afficherErreur("erreur-connexion", message || null);
  afficherEcran("connexion");
}

$("btn-deconnexion").addEventListener("click", () => deconnecter());


/* =========================================================
   ECRAN 2 : LISTE DES UTILISATEURS
   ========================================================= */

function trouverUtilisateur(id) {
  return etat.utilisateurs.find((u) => u.id === id);
}

async function chargerUtilisateurs() {
  try {
    const liste = await appeler("/api/utilisateurs");
    if (!etat.session) return;
    const anciensNonLus = new Map(etat.utilisateurs.map((u) => [u.id, u.nonLus]));
    etat.utilisateurs = liste
      .filter((u) => u.id !== etat.session.id)
      .map((u) => ({ ...u, nonLus: anciensNonLus.get(u.id) || 0 }));
    afficherErreur("erreur-liste", null);
    afficherListe();
    if (etat.contactId != null) majEnteteConversation();
  } catch (erreur) {
    if (!estSessionExpiree(erreur)) afficherErreur("erreur-liste", erreur.message);
  }
}

$("btn-rafraichir").addEventListener("click", chargerUtilisateurs);

function afficherListe() {
  const ul = $("liste-utilisateurs");
  ul.replaceChildren();
  $("liste-vide").hidden = etat.utilisateurs.length > 0;

  // Les utilisateurs en ligne d'abord, puis par ordre alphabetique
  const tries = [...etat.utilisateurs].sort(
    (a, b) => (b.connecte - a.connecte) || a.username.localeCompare(b.username, "fr")
  );

  for (const u of tries) {
    const bouton = document.createElement("button");
    bouton.type = "button";
    bouton.className = "user-item";
    bouton.addEventListener("click", () => ouvrirConversation(u.id));

    const avatar = document.createElement("span");
    avatar.className = "avatar";
    avatar.setAttribute("aria-hidden", "true");
    avatar.textContent = initiale(u.username);
    const pastille = document.createElement("span");
    pastille.className = u.connecte ? "pastille en-ligne" : "pastille";
    avatar.append(pastille);

    const texte = document.createElement("span");
    texte.className = "user-texte";
    const nom = document.createElement("span");
    nom.className = "user-nom";
    nom.textContent = u.username;
    const statut = document.createElement("span");
    statut.className = "user-statut";
    statut.textContent = etat.ecritureDe === u.id
      ? "est en train d'écrire..."
      : (u.connecte ? "en ligne" : "hors ligne");
    texte.append(nom, statut);

    bouton.append(avatar, texte);

    if (u.nonLus > 0) {
      const badge = document.createElement("span");
      badge.className = "badge";
      badge.textContent = u.nonLus;
      badge.setAttribute("aria-label", `${u.nonLus} message(s) non lu(s)`);
      bouton.append(badge);
    }

    const li = document.createElement("li");
    li.append(bouton);
    ul.append(li);
  }
}


/* =========================================================
   ECRAN 3 : CONVERSATION
   ========================================================= */

function ouvrirConversation(id) {
  const u = trouverUtilisateur(id);
  if (!u) return;

  etat.contactId = id;
  etat.messages = [];
  u.nonLus = 0;
  // On repart de zero : l'indicateur d'une autre conversation ne doit pas s'afficher ici
  etat.ecritureDe = null;
  clearTimeout(etat.minuteurEcriture);

  afficherErreur("erreur-conversation", null);
  $("champ-message").value = "";
  majEnteteConversation();
  afficherMessages();
  majSaisie();
  afficherEcran("conversation");
  chargerHistorique();
}

$("btn-retour").addEventListener("click", () => {
  etat.contactId = null;
  etat.messages = [];
  afficherEcran("liste");
  afficherListe();
});

/** Historique REST de la conversation ouverte, fusionne avec les messages deja recus. */
async function chargerHistorique() {
  const id = etat.contactId;
  if (id == null) return;
  try {
    const liste = await appeler(`/api/messages/${id}`);
    if (etat.contactId !== id) return; // conversation quittee entre-temps
    fusionnerMessages(liste);
  } catch (erreur) {
    if (!estSessionExpiree(erreur) && etat.contactId === id) {
      afficherErreur("erreur-conversation", erreur.message);
    }
  }
}

/** Ajoute des messages sans doublon (comparaison par id), tries par id. */
function fusionnerMessages(ajouts) {
  const ids = new Set(etat.messages.map((m) => m.id));
  etat.messages = [...etat.messages, ...ajouts.filter((m) => !ids.has(m.id))]
    .sort((a, b) => a.id - b.id);
  afficherMessages();
}

function afficherMessages() {
  const zone = $("liste-messages");
  zone.replaceChildren();

  if (etat.messages.length === 0) {
    const vide = document.createElement("p");
    vide.className = "aucun-message";
    vide.textContent = "Aucun message pour le moment.";
    zone.append(vide);
    return;
  }

  for (const m of etat.messages) {
    const mien = m.expediteurId === etat.session.id;
    const bulle = document.createElement("div");
    bulle.className = mien ? "message mien" : "message autre";

    const texte = document.createElement("p");
    texte.textContent = m.contenu; // textContent : aucun HTML injecte depuis un message

    const heure = document.createElement("span");
    heure.className = "message-heure";
    heure.textContent = formaterHeure(m.dateEnvoi);

    bulle.append(texte, heure);
    zone.append(bulle);
  }
  zone.scrollTop = zone.scrollHeight;
}

/** Nom, avatar et statut du contact ; le statut devient "est en train d'ecrire..." quand il ecrit. */
function majEnteteConversation() {
  const u = trouverUtilisateur(etat.contactId);
  if (!u) return;
  $("conv-nom").textContent = u.username;
  $("conv-avatar").textContent = initiale(u.username);

  const statut = $("conv-statut");
  if (etat.ecritureDe === u.id) {
    statut.textContent = "est en train d'écrire...";
    statut.className = "conv-statut ecrit";
  } else {
    statut.textContent = u.connecte ? "en ligne" : "hors ligne";
    statut.className = u.connecte ? "conv-statut en-ligne" : "conv-statut";
  }
}

function majSaisie() {
  const champ = $("champ-message");
  champ.disabled = !etat.canalOuvert;
  champ.placeholder = etat.canalOuvert ? "Écris un message..." : "Connexion au canal en cours...";
  $("btn-envoyer").disabled = !etat.canalOuvert || !champ.value.trim();
}

/* Signale la saisie au serveur, au plus une fois toutes les 2 secondes */
$("champ-message").addEventListener("input", (evenement) => {
  majSaisie();
  const maintenant = Date.now();
  if (evenement.target.value && maintenant - etat.dernierSignalEcriture > DELAI_SIGNAL_ECRITURE_MS) {
    etat.dernierSignalEcriture = maintenant;
    envoyer({ type: "typing", destinataireId: etat.contactId });
  }
});

/*
 * Envoie le message au serveur : il revient par le canal avec son id et sa date
 * officielle, et c'est ce retour qui ajoute la bulle.
 */
$("form-message").addEventListener("submit", (evenement) => {
  evenement.preventDefault();
  const champ = $("champ-message");
  const contenu = champ.value.trim();
  if (!contenu) return;

  afficherErreur("erreur-conversation", null);
  const envoye = envoyer({ type: "message", destinataireId: etat.contactId, contenu });
  if (!envoye) {
    afficherErreur("erreur-conversation", "Canal fermé : le message n'a pas été envoyé.");
    return;
  }
  champ.value = "";
  majSaisie();
  champ.focus();
});


/* =========================================================
   CANAL TEMPS REEL (WEBSOCKET)
   Ouvert avec le jeton JWT, rouvert automatiquement s'il se ferme
   (serveur redemarre, reseau coupe), avec une attente de plus en plus longue.
   ========================================================= */

function envoyer(objet) {
  const socket = etat.socket;
  if (socket && socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(objet));
    return true;
  }
  return false;
}

function ouvrirCanal() {
  fermerCanal();
  etat.canalActif = true;
  etat.tentative = 0;
  connecterCanal();
}

function connecterCanal() {
  if (!etat.canalActif || !etat.session) return;

  const socket = new WebSocket(`${URL_WS}?token=${encodeURIComponent(etat.session.token)}`);
  etat.socket = socket;

  socket.addEventListener("open", () => {
    etat.tentative = 0;
    majCanal(true);
    // Le canal vient de s'ouvrir : on recharge la liste pour avoir les statuts a jour,
    // et l'historique, pour recuperer les messages recus pendant une coupure
    chargerUtilisateurs();
    if (etat.contactId != null) chargerHistorique();
  });

  socket.addEventListener("close", () => {
    if (etat.socket !== socket) return; // ancien canal deja remplace
    etat.socket = null;
    majCanal(false);
    if (!etat.canalActif) return;
    const delai = Math.min(1000 * 2 ** etat.tentative, DELAI_MAX_RECONNEXION_MS);
    etat.tentative += 1;
    etat.minuteurReconnexion = setTimeout(connecterCanal, delai);
  });

  socket.addEventListener("message", (evenement) => {
    let donnees;
    try { donnees = JSON.parse(evenement.data); } catch { return; } // message non JSON : ignore
    traiterEvenement(donnees);
  });
}

function fermerCanal() {
  etat.canalActif = false;
  clearTimeout(etat.minuteurReconnexion);
  const socket = etat.socket;
  etat.socket = null;
  if (socket) socket.close();
  majCanal(false);
}

function majCanal(ouvert) {
  etat.canalOuvert = ouvert;
  const pastille = $("pastille-canal");
  pastille.className = ouvert ? "pastille en-ligne" : "pastille";
  pastille.title = ouvert ? "Canal temps réel ouvert" : "Canal temps réel fermé";
  $("bandeau-canal").hidden = ouvert || !etat.session;
  majSaisie();
}

/** Le client lit toujours le champ "type" en premier (contrat, section 3.2). */
function traiterEvenement(evenement) {
  switch (evenement.type) {
    case "presence":
      recevoirPresence(evenement);
      break;
    case "message":
      recevoirMessage(evenement);
      break;
    case "typing":
      recevoirEcriture(evenement);
      break;
    case "erreur":
      if (etat.contactId != null) {
        afficherErreur("erreur-conversation", evenement.message);
      } else {
        afficherToast(evenement.message || "Une erreur est survenue.");
      }
      break;
  }
}

function recevoirPresence(p) {
  if (p.utilisateurId === etat.session?.id) return;
  const u = trouverUtilisateur(p.utilisateurId);
  if (!u) {
    chargerUtilisateurs(); // utilisateur cree apres le chargement de la liste
    return;
  }
  u.connecte = p.connecte;
  afficherListe();
  if (etat.contactId === u.id) majEnteteConversation();
}

function recevoirMessage(m) {
  const moi = etat.session.id;
  const autreId = m.expediteurId === moi ? m.destinataireId : m.expediteurId;
  const recu = m.expediteurId !== moi;

  if (recu) {
    // Le message est arrive : l'autre a fini d'ecrire, et on previent par un son
    if (etat.ecritureDe === m.expediteurId) {
      etat.ecritureDe = null;
      clearTimeout(etat.minuteurEcriture);
    }
    jouerSon();
  }

  // Conversation ouverte avec cette personne : on ajoute la bulle
  if (autreId === etat.contactId) {
    fusionnerMessages([m]);
    majEnteteConversation();
    return;
  }

  // Message recu dans une autre conversation : compteur de non lus + notification
  if (recu) {
    const u = trouverUtilisateur(autreId);
    if (u) {
      u.nonLus += 1;
      afficherListe();
      afficherToast(`Nouveau message de ${u.username}`);
    } else {
      chargerUtilisateurs().then(() => {
        const nouveau = trouverUtilisateur(autreId);
        if (nouveau) {
          nouveau.nonLus += 1;
          afficherListe();
        }
      });
    }
  }
}

/* "est en train d'ecrire" : s'efface tout seul apres 3 secondes sans nouvelle saisie */
function recevoirEcriture(t) {
  etat.ecritureDe = t.expediteurId;
  clearTimeout(etat.minuteurEcriture);
  etat.minuteurEcriture = setTimeout(() => {
    etat.ecritureDe = null;
    majEnteteConversation();
    afficherListe();
  }, DUREE_INDICATEUR_ECRITURE_MS);
  majEnteteConversation();
  afficherListe();
}


/* =========================================================
   DEMARRAGE
   Si une session existe deja dans l'onglet, on la reprend.
   ========================================================= */
majModeConnexion();
const sessionExistante = lireSession();
if (sessionExistante?.token) {
  demarrerSession(sessionExistante);
} else {
  afficherEcran("connexion");
}
