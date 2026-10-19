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

    function jouerSon() {
        const audio = new Audio('/notification.mp3')
        audio.play()
            .then(() => console.log('Son joué'))
            .catch((e) => console.warn('Son bloqué par le navigateur :', e.name))
    }

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

    function surEvenement(evenement) {
        if (evenement.type === 'ouverture') {
            charger()
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
            const autreId =
                evenement.expediteurId === session.id ? evenement.destinataireId : evenement.expediteurId
            if (autreId === contactId) {
                setMessages((courants) => fusionner(courants, [evenement]))
            }
            if (evenement.expediteurId !== session.id) {
                setEcritureDe(null)
                clearTimeout(minuteurEcriture.current)
                jouerSon()
            }
        } else if (evenement.type === 'typing') {
            setEcritureDe(evenement.expediteurId)
            clearTimeout(minuteurEcriture.current)
            minuteurEcriture.current = setTimeout(() => setEcritureDe(null), 4000)
        } else if (evenement.type === 'erreur') {
            setErreurConversation(evenement.message)
        }
    }

    const { connecte: canalOuvert, envoyer } = useChatSocket(session?.token, surEvenement)

    function envoyerMessage(contenu) {
        setErreurConversation('')
        const envoye = envoyer({ type: 'message', destinataireId: contactId, contenu })
        if (!envoye) {
            setErreurConversation("Canal fermé : le message n'a pas été envoyé.")
        }
        return envoye
    }

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
                                {ecritureDe === u.id && <span className="user-ecrit">écrit...</span>}
                            </li>
                        ))}
                </ul>
            </aside>
            {contact ? (
                <main className="chat-window">
                    <Conversation
                        key={contact.id}
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