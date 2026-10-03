
import { API_URL } from './config'

const CLE_SESSION = 'messagerie.session'

// La session (jeton, id, nom) est gardee dans sessionStorage : elle est propre
// a l'onglet, ce qui permet de tester deux utilisateurs dans deux onglets.
export function lireSession() {
    try {
        return JSON.parse(sessionStorage.getItem(CLE_SESSION))
    } catch {
        return null
    }
}

export function ecrireSession(session) {
    sessionStorage.setItem(CLE_SESSION, JSON.stringify(session))
}

export function effacerSession() {
    sessionStorage.removeItem(CLE_SESSION)
}

// Appel REST generique : ajoute le jeton, lit le JSON et transforme les
// erreurs du serveur ({status, erreur, message, date}) en exception.
async function appeler(chemin, { methode = 'GET', corps, jeton } = {}) {
    const entetes = { 'Content-Type': 'application/json' }
    if (jeton) {
        entetes.Authorization = `Bearer ${jeton}`
    }

    let reponse
    try {
        reponse = await fetch(`${API_URL}${chemin}`, {
            method: methode,
            headers: entetes,
            body: corps ? JSON.stringify(corps) : undefined,
        })
    } catch {
        throw new Error('Serveur injoignable. Verifie qu\'il est lance sur le port 8080.')
    }

    const donnees = await reponse.json().catch(() => null)

    if (!reponse.ok) {
        const erreur = new Error(donnees?.message || `Erreur ${reponse.status}`)
        erreur.status = reponse.status
        throw erreur
    }
    return donnees
}

export const inscrire = (username, motDePasse) =>
    appeler('/api/comptes', { methode: 'POST', corps: { username, motDePasse } })

export const connecter = (username, motDePasse) =>
    appeler('/api/auth/login', { methode: 'POST', corps: { username, motDePasse } })

export const listerUtilisateurs = (jeton) =>
    appeler('/api/utilisateurs', { jeton })

export const recupererHistorique = (jeton, utilisateurId) =>
    appeler(`/api/messages/${utilisateurId}`, { jeton })