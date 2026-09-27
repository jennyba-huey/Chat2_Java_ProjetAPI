# Projet Messagerie instantanée

Projet de synthèse API et Web Services — EFREI Paris

## Description

Ce projet consiste à développer une application de messagerie instantanée permettant à deux utilisateurs de communiquer en temps réel depuis deux appareils différents.

L'application repose sur :

* une API REST développée avec Spring Boot ;
* une communication temps réel avec WebSocket ;
* une authentification sécurisée avec JWT ;
* une base de données MySQL ;
* un client bureau développé avec React/Vite ;
* un client mobile développé en HTML, CSS et JavaScript.

Les messages sont enregistrés en base de données afin de conserver l'historique des conversations.

## Technologies utilisées

### Serveur

* Java 17
* Spring Boot 3.5.11
* Maven
* Spring Web
* Spring Data JPA
* MySQL
* BCrypt
* JWT
* WebSocket

### Client bureau

* React
* Vite
* JavaScript

### Client mobile

* HTML
* CSS
* JavaScript

### Tests

* Postman

## Structure du projet

```text
projet-messagerie/
│
├── serveur/
│   └── APIProject/
│       └── Projet Spring Boot
│
├── client-mobile/
│   ├── index.html
│   ├── style.css
│   └── app.js
│
├── client-bureau/
│   └── Application React/Vite
│
├── postman/
│   └── Collection ProjetAPI
│
├── rapport/
│   └── Documents et captures du projet
│
└── README.md
```

## API REST

L'API est accessible localement à l'adresse :

```text
http://localhost:8080
```

### Création d'un compte

```http
POST /api/comptes
```

Corps de la requête :

```json
{
  "username": "alice",
  "motDePasse": "motdepasse"
}
```

### Connexion

```http
POST /api/auth/login
```

Corps :

```json
{
  "username": "alice",
  "motDePasse": "motdepasse"
}
```

La connexion retourne un JWT.

### Liste des utilisateurs

```http
GET /api/utilisateurs
```

Cette requête nécessite un jeton JWT.

### Historique d'une conversation

```http
GET /api/messages/{id}
```

Cette requête permet de récupérer l'historique des messages avec un utilisateur.

Les requêtes protégées utilisent l'en-tête :

```text
Authorization: Bearer <jeton>
```

## WebSocket

La communication en temps réel utilise WebSocket.

En local :

```text
ws://localhost:8080/ws/messages?token=<jeton>
```

Les messages sont échangés au format JSON.

Exemple :

```json
{
  "type": "message",
  "expediteurId": 1,
  "destinataireId": 2,
  "contenu": "Bonjour !",
  "dateEnvoi": "2026-09-21T10:15:30"
}
```

Le serveur enregistre le message en base puis l'envoie au destinataire lorsqu'il est connecté.

## Authentification

L'application utilise JWT pour sécuriser les endpoints protégés.

Les endpoints suivants restent accessibles sans authentification :

```text
POST /api/comptes
POST /api/auth/login
```

Les autres endpoints nécessitent un JWT valide.

Le mot de passe des utilisateurs est stocké sous forme hachée avec BCrypt.

## Base de données

Le projet utilise une base de données MySQL nommée :

```text
messagerie
```

Le mot de passe MySQL ne doit pas être envoyé sur GitHub.

La variable d'environnement suivante est utilisée :

```text
DB_PASSWORD
```

Dans `application.properties` :

```properties
spring.datasource.password=${DB_PASSWORD:}
```

Chaque développeur doit donc définir sa propre variable d'environnement `DB_PASSWORD`.

## Installation

### Prérequis

Avant de lancer le projet, installer :

* Java 17
* Maven
* MySQL
* Node.js
* npm
* Postman
* IntelliJ IDEA ou un IDE équivalent

### 1. Cloner le projet

```bash
git clone <URL_DU_REPOSITORY>
```

Puis entrer dans le projet :

```bash
cd projet-messagerie
```

### 2. Lancer le serveur

Ouvrir le dossier :

```text
serveur/APIProject
```

avec IntelliJ IDEA.

Configurer la variable d'environnement :

```text
DB_PASSWORD=<mot_de_passe_mysql>
```

Puis lancer :

```text
ApiProjectApplication
```

Le serveur démarre sur :

```text
http://localhost:8080
```

### 3. Lancer le client bureau

Depuis le dossier du client React :

```bash
npm install
npm run dev
```

### 4. Lancer le client mobile

Le client mobile peut être lancé avec un serveur local, par exemple avec VS Code Live Server.

L'adresse du serveur API doit être adaptée à l'adresse IP du PC qui héberge le serveur lors des tests sur plusieurs appareils.

## Tests

Une collection Postman `ProjetAPI` est fournie dans le dossier :

```text
postman/
```

Elle permet notamment de tester :

* la création d'un compte ;
* la connexion ;
* l'authentification JWT ;
* la récupération des utilisateurs ;
* la récupération de l'historique ;
* les erreurs HTTP ;
* la communication WebSocket.

## Communication entre deux appareils

Pour tester la messagerie entre un téléphone et un ordinateur :

1. Les deux appareils doivent être connectés au même réseau Wi-Fi.
2. Le serveur Spring Boot doit être lancé sur le PC.
3. Récupérer l'adresse IPv4 du PC avec :

```bash
ipconfig
```

4. Utiliser cette adresse IP dans les clients.
5. Vérifier que le port `8080` est autorisé par le pare-feu Windows.
6. Tester l'envoi d'un message entre les deux appareils.

Exemple :

```text
http://192.168.1.20:8080
```

et :

```text
ws://192.168.1.20:8080/ws/messages?token=<jeton>
```

## Fonctionnalités prévues

### Fonctionnalités obligatoires

* Création de compte
* Connexion avec JWT
* Sécurisation des endpoints
* Messagerie en temps réel
* Enregistrement des messages dans MySQL
* Historique des conversations
* Statut connecté / hors ligne
* Client bureau
* Client mobile

### Fonctionnalités supplémentaires

* Notification hors ligne simulée
* Indicateur « en train d'écrire »
* Documentation Swagger
* Limitation du nombre de messages envoyés

## Organisation Git

Chaque membre travaille sur sa propre branche.

Branches prévues :

```text
main
djeneba
binome
```

Avant de commencer une session :

```bash
git pull
```

Après avoir terminé une fonctionnalité :

```bash
git add .
git commit -m "Description de la modification"
git push
```

Les modifications fonctionnelles sont ensuite fusionnées dans `main`.

## Autrices 👩🏾‍💻 🧑🏾‍💻

Projet réalisé dans le cadre du Master Ingénierie Informatique à l'EFREI Paris.

* Djeneba Ba
* Ashley Makuiza


Version : septembre 2026
