# Messagerie instantanée entre deux appareils

Projet de synthèse **API et Web Services** — Master Ingénierie Informatique, EFREI Paris
Binôme : **Djeneba Ba** et **Ashley Kilola Makuiza** — Enseignant : Ralph Bou Nader

## En deux mots

C'est une petite application de chat, comme une messagerie sur téléphone, mais faite par nous de A à Z.
Deux personnes, sur deux appareils différents (par exemple un téléphone et un ordinateur), peuvent :

- créer un compte et se connecter ;
- voir qui est en ligne ;
- s'envoyer des messages qui arrivent **instantanément**, sans recharger la page ;
- retrouver tout l'historique de leur conversation, même après s'être déconnectées ;
- recevoir les messages envoyés pendant qu'elles étaient déconnectées.

Tous les messages passent par un **serveur central** et sont enregistrés dans une **base de données**.
Les deux appareils ne se parlent jamais directement : c'est le serveur qui fait le facteur.

## Comment ça marche (version simple)

```
   Téléphone                    Serveur Spring Boot                  Ordinateur
 (client mobile)                   (port 8080)                    (client bureau)
       │                               │                                │
       │── REST : connexion, liste ───►│◄─── REST : connexion, liste ───│
       │   des utilisateurs, historique│     des utilisateurs, historique│
       │                               │                                │
       │◄═══ WebSocket : messages ════►│◄════ WebSocket : messages ════►│
       │     en direct                 │      en direct                 │
                                       │
                                  Base MySQL
                          (comptes + tous les messages)
```

On utilise deux façons de communiquer, chacune pour ce qu'elle fait de mieux :

- **REST (HTTP)** : on pose une question, le serveur répond, et c'est fini. Parfait pour se connecter
  ou récupérer l'historique, qui sont des actions ponctuelles.
- **WebSocket** : la connexion reste ouverte, comme un appel téléphonique. Le serveur peut donc
  envoyer un nouveau message au destinataire dès qu'il arrive, sans que celui-ci ait rien demandé.

## Technologies

| Partie | Technologies |
|---|---|
| Serveur | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, WebSocket |
| Base de données | MySQL |
| Sécurité | Mots de passe hachés avec BCrypt, authentification par jeton JWT |
| Client bureau | React + Vite (JavaScript) |
| Client mobile | HTML, CSS et JavaScript, sans framework |
| Documentation et tests | Swagger, Postman, JUnit |

## Organisation du dépôt

```
Chat2_Java_ProjetAPI/
├── serveur/          le serveur Spring Boot (l'API)
├── client-bureau/    le client pour ordinateur (React)
├── client-mobile/    le client pour téléphone (HTML/CSS/JS)
├── postman/          la collection Postman pour tester l'API
├── rapport/          les captures et brouillons du rapport
└── README.md         ce fichier
```

## Ce qu'il faut installer

- **Java 17** (attention : pas une version plus récente, le projet est prévu pour la 17)
- **MySQL** (version 8 ou plus)
- **Node.js** (pour le client bureau)
- **Python 3** (déjà présent sur Mac, pour servir le client mobile) ou l'extension **Live Server** de VS Code
- Conseillé : **IntelliJ IDEA** pour le serveur, **VS Code** pour les clients, **Postman** pour les tests

> 💡 Sur Mac, ne mettez pas le projet dans **Documents** ou **Bureau** si ces dossiers sont
> synchronisés avec iCloud : iCloud bloque Git, IntelliJ et Vite au mauvais moment.
> Un dossier comme `~/projets` évite ce problème.

## Lancer le projet

Il faut lancer **trois choses** : le serveur, puis les deux clients.

### 1. Le serveur

1. Démarrez MySQL (sur Mac avec Homebrew : `brew services start mysql`).
2. Ouvrez le dossier `serveur` dans IntelliJ : **File → Open**, choisissez `serveur/pom.xml`, puis **Open as Project**.
3. Vérifiez que le projet utilise bien **Java 17** : **File → Project Structure → Project → SDK**.
4. Donnez votre mot de passe MySQL au serveur. Il n'est **pas écrit dans le code** (pour qu'il ne finisse pas sur GitHub) :
   **Run → Edit Configurations → ApiProjectApplication → Modify options → Environment variables**,
   puis écrivez `DB_PASSWORD=votre_mot_de_passe_mysql`.
5. Lancez `ApiProjectApplication` avec le triangle vert.

C'est bon quand la console affiche `Started ApiProjectApplication`.
La base `messagerie` et ses tables se créent toutes seules au premier lancement.

### 2. Le client bureau (ordinateur)

Dans un terminal :

```bash
cd client-bureau
npm install      # seulement la première fois : télécharge React, Vite, etc.
npm run dev
```

Puis ouvrez **http://localhost:5173**.

### 3. Le client mobile (téléphone)

Dans un autre terminal :

```bash
cd client-mobile
python3 -m http.server 5500
```

Puis ouvrez **http://localhost:5500** (ou utilisez **Open with Live Server** dans VS Code, en ouvrant
uniquement le dossier `client-mobile`).

## Faire la démo sur deux vrais appareils

1. Branchez l'ordinateur qui fait tourner le serveur et le téléphone sur **le même Wi-Fi**.
2. Trouvez l'adresse IP de l'ordinateur :
   - Mac : `ipconfig getifaddr en0`
   - Windows : `ipconfig`, ligne **Adresse IPv4** de la carte Wi-Fi
3. Sur le téléphone, ouvrez `http://<adresse-IP>:5500` (par exemple `http://192.168.1.103:5500`).
4. Sur l'ordinateur, ouvrez le client bureau, connectez-vous avec un autre compte, et écrivez-vous !

**Pas besoin de modifier le code pour changer d'IP** : les deux clients prennent automatiquement
l'adresse de la page pour trouver le serveur. Si jamais la page du client mobile est servie par une
autre machine que le serveur, il suffit de modifier la constante `SERVEUR` tout en haut de `client-mobile/app.js`.

**Si le téléphone n'arrive pas à se connecter :**
- le pare-feu de l'ordinateur bloque peut-être les connexions : autorisez Java et Python
  (sur Mac : **Réglages Système → Réseau → Coupe-feu**) ;
- certains Wi-Fi (écoles, Wi-Fi « invité ») empêchent les appareils de se voir entre eux :
  dans ce cas, activez le **partage de connexion** du téléphone et connectez l'ordinateur dessus.

## Comptes de test

La base de données est **locale** : chaque ordinateur a la sienne, vide au départ.
Créez d'abord deux comptes, avec le bouton **Créer un compte** d'un des clients, ou avec Postman.
Par exemple :

| Nom d'utilisateur | Mot de passe |
|---|---|
| Jenny | motdepasse07 |
| Ashley | secret123 |

Règles : au moins 3 caractères pour le nom, au moins 6 pour le mot de passe.

## Tester l'API sans les clients

- **Swagger** : avec le serveur lancé, ouvrez **http://localhost:8080/swagger-ui.html**.
  C'est une page qui liste toutes les adresses de l'API et permet de les essayer directement.
  Pour les adresses protégées (cadenas) : lancez d'abord **POST /api/auth/login**, copiez le `token`
  reçu, cliquez sur **Authorize** et collez-le.
- **Postman** : importez `postman/ProjetAPI.postman_collection.json`. Lancez toujours une requête de
  **login** en premier : son script enregistre le jeton tout seul pour les autres requêtes.
- **Tests automatiques** : dans IntelliJ, lancez `RateLimitServiceTest` (dans `serveur/src/test`).

## L'API en résumé

| Adresse | Jeton ? | À quoi ça sert |
|---|---|---|
| `POST /api/comptes` | Non | Créer un compte |
| `POST /api/auth/login` | Non | Se connecter et recevoir un jeton |
| `GET /api/utilisateurs` | Oui | Liste des utilisateurs, avec qui est en ligne |
| `GET /api/messages/{id}` | Oui | Historique de la conversation avec l'utilisateur `{id}` |
| `ws://…:8080/ws/messages?token=…` | Oui | Canal temps réel (envoi et réception des messages) |

Le jeton s'envoie dans l'en-tête `Authorization: Bearer <jeton>`.
Sans jeton valide, le serveur répond `401`.

## Si le destinataire est hors ligne

- Le message est **enregistré en base avant d'être envoyé** : rien n'est perdu.
- Le serveur simule une notification (webhook) : une ligne `[WEBHOOK] Notification envoyee…`
  apparaît dans la console du serveur.
- À sa prochaine connexion, le destinataire retrouve le message dans son historique.

## La sécurité, en bref

- Les mots de passe ne sont **jamais stockés en clair** : ils sont hachés avec BCrypt.
- Au login, le serveur donne un **jeton JWT** (valable 24 h), une sorte de badge signé.
  Le client le montre à chaque requête. Si quelqu'un modifie le jeton, la signature ne correspond
  plus et le serveur le refuse.
- Le jeton est aussi vérifié à l'ouverture du WebSocket.
- Le serveur décide lui-même **qui envoie** un message (à partir du jeton) et **à quelle heure** :
  impossible de se faire passer pour quelqu'un d'autre.
- **Anti-spam** : un utilisateur ne peut pas envoyer plus de 10 messages en 10 secondes.
  Les messages refusés ne sont pas enregistrés.
- **Taille des messages** : un message vide ou de plus de 2000 caractères est refusé par le serveur.

Limites assumées pour une démo en local : la clé de signature est dans la configuration,
le jeton est visible dans l'adresse du WebSocket, et on utilise `ws://` au lieu de `wss://` (non chiffré).

## Problèmes fréquents

| Problème | Solution |
|---|---|
| `Access denied for user 'root'` au lancement | Le mot de passe dans `DB_PASSWORD` est faux |
| `Communications link failure` | MySQL n'est pas démarré |
| `Port 8080 already in use` | Un autre serveur tourne déjà : arrêtez-le |
| Le client affiche « Serveur injoignable » | Le serveur n'est pas lancé, ou mauvaise adresse IP |
| Live Server n'apparaît pas dans VS Code | VS Code est en « Restricted Mode » : cliquez dessus puis **Trust** |
| La page du client mobile se recharge toute seule | Ouvrez uniquement le dossier `client-mobile` dans VS Code, pas tout le projet |

## Qui a fait quoi

| | Djeneba | Ashley |
|---|---|---|
| Serveur | Entités, API REST de base, canal WebSocket, présence, branchement de l'anti-spam | Sécurité JWT, CORS, vérification du jeton du WebSocket, service anti-spam |
| Clients | Client bureau (React) | Client mobile (HTML/CSS/JS) |
| Bonus | Indicateur « en train d'écrire », notification hors ligne (webhook simulé) | Swagger, limitation de débit (anti-spam), tests automatiques |
