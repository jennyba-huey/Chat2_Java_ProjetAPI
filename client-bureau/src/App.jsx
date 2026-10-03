import { useState, useEffect, useCallback, useRef } from 'react'
import LoginForm from './LoginForm'
import Conversation from './Conversation'
import useChatSocket from './useChatSocket'
import {
    lireSession,
    ecrireSession,
    effacerSession,
    listerUtilisateurs,
    recupererHistorique,
} from './api'

// Ajoute des messages a une liste sans doublon (comparaison par id), tries par id
function fusionner(liste, ajouts) {
    const ids = new Set(liste.map((m) => m.id))
    return [...liste, ...ajouts.filter((m) => !ids.has(m.id))].sort((a, b) => a.id - b.id)
}

export default function App() {
    const [session, setSession] = useState(lireSession)
    const [utilisateurs, setUtilisateurs] = useState([])
    const [erreur, setErreur] = useState('')
    const [contactId, setContactId] = useState(null)
    const [messages, setMessages] = useState([])
    const [erreurConversation, setErreurConversation] = useState('')
    const [ecritureDe, setEcritureDe] = useState(null)
    const minuteurEcriture = useRef(null)

    function deconnecter() {
        effacerSession()
        setSession(null)
        setUtilisateurs([])
        setContactId(null)
        setMessages([])
    }

    function surConnexion(nouvelle) {
        ecrireSession(nouvelle)
        setSession(nouvelle)
    }

    const charger = useCallback(async () => {
        try {
            setUtilisateurs(await listerUtilisateurs(session.token))
            setErreur('')
        } catch (err) {
            // Jeton expire ou invalide : retour a l'ecran de connexion
            if (err.status === 401) {
                deconnecter()
            } else {
                setErreur(err.message)
            }
        }
    }, [session])

    useEffect(() => {
        if (session) charger()
    }, [session, charger])

    // Charge l'historique REST a chaque changement de contact
    const jeton = session?.token
    useEffect(() => {
        if (!jeton || contactId == null) return undefined
        let actif = true
        setMessages([])
        setErreurConversation('')
        recupererHistorique(jeton, contactId)
            .then((liste) => {
                if (actif) setMessages((courants) => fusionner(liste, courants))
            })
            .catch((err) => {
                if (actif) setErreurConversation(err.message)
            })
        return () => {
            actif = false
        }
    }, [jeton, contactId])

    // Evenements recus par le canal temps reel
    function surEvenement(evenement) {
        if (evenement.type === 'ouverture') {
            // Le canal vient de s'ouvrir : on recharge la liste pour avoir les statuts a jour
            charger()
            // et l'historique, pour recuperer les messages recus pendant une coupure
            if (contactId != null) {
                recupererHistorique(session.token, contactId)
                    .then((liste) => setMessages((courants) => fusionner(liste, courants)))
                    .catch(() => {})
            }
        } else if (evenement.type === 'presence') {
            setUtilisateurs((liste) =>
                liste.map((u) =>
                    u.id === evenement.utilisateurId ? { ...u, connecte: evenement.connecte } : u
                )
            )
        } else if (evenement.type === 'message') {
            // On n'affiche que les messages de la conversation ouverte
            const autreId =
                evenement.expediteurId === session.id ? evenement.destinataireId : evenement.expediteurId
            if (autreId === contactId) {
                setMessages((courants) => fusionner(courants, [evenement]))
            }
            // Le message est arrive : l'autre a fini d'ecrire, et on previent par un son
            if (evenement.expediteurId !== session.id) {
                setEcritureDe(null)
                new Audio('/notification.mp3').play().catch(() => {})
            }
        } else if (evenement.type === 'typing') {
            // "est en train d'ecrire" : s'efface tout seul apres 3 secondes sans nouvelle saisie
            setEcritureDe(evenement.expediteurId)
            clearTimeout(minuteurEcriture.current)
            minuteurEcriture.current = setTimeout(() => setEcritureDe(null), 3000)
        } else if (evenement.type === 'erreur') {
            setErreurConversation(evenement.message)
        }
    }

    const { connecte: canalOuvert, envoyer } = useChatSocket(session?.token, surEvenement)

    // Envoie le message au serveur : il revient par le canal avec son id et sa date
    function envoyerMessage(contenu) {
        setErreurConversation('')
        const envoye = envoyer({ type: 'message', destinataireId: contactId, contenu })
        if (!envoye) {
            setErreurConversation("Canal fermé : le message n'a pas été envoyé.")
        }
        return envoye
    }

    // Previent le destinataire qu'on est en train d'ecrire
    function signalerSaisie() {
        envoyer({ type: 'typing', destinataireId: contactId })
    }

    if (!session) {
        return <LoginForm onConnecte={surConnexion} />
    }

    const contact = utilisateurs.find((u) => u.id === contactId)

    return (
        <div className="app-container">
            <aside className="sidebar">
                <div className="sidebar-header">
          <span className="nom-session">
            <span
                className={canalOuvert ? 'pastille en-ligne' : 'pastille'}
                title={canalOuvert ? 'Canal temps réel ouvert' : 'Canal temps réel fermé'}
            />
            <strong>{session.username}</strong>
          </span>
                    <button className="lien" onClick={deconnecter}>Déconnexion</button>
                </div>
                <button className="lien" onClick={charger}>Rafraîchir la liste</button>
                {erreur && <p className="erreur">{erreur}</p>}
                <ul className="user-list">
                    {utilisateurs
                        .filter((u) => u.id !== session.id)
                        .map((u) => (
                            <li
                                key={u.id}
                                className={u.id === contactId ? 'user-item actif' : 'user-item'}
                                onClick={() => setContactId(u.id)}
                            >
                                <span className={u.connecte ? 'pastille en-ligne' : 'pastille'} />
                                {u.username}
                            </li>
                        ))}
                </ul>
            </aside>
            {contact ? (
                <main className="chat-window">
                    <Conversation
                        session={session}
                        contact={contact}
                        messages={messages}
                        erreur={erreurConversation}
                        canalOuvert={canalOuvert}
                        ecrit={ecritureDe === contactId}
                        onEnvoyer={envoyerMessage}
                        onSaisie={signalerSaisie}
                    />
                </main>
            ) : (
                <main className="chat-window vide">
                    <p>Choisis un contact pour démarrer la conversation.</p>
                </main>
            )}
        </div>
    )
}