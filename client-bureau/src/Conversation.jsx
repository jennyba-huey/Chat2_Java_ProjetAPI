import { useState, useEffect, useRef } from 'react'

function formaterHeure(dateIso) {
    return new Date(dateIso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}

export default function Conversation({ session, contact, messages, erreur, canalOuvert, ecrit, onEnvoyer, onSaisie }) {
    const [texte, setTexte] = useState('')
    const bas = useRef(null)
    const dernierSignal = useRef(0)

    // Descend en bas de la liste quand les messages changent
    useEffect(() => {
        bas.current?.scrollIntoView()
    }, [messages])

    // Signale la saisie au serveur, au plus une fois toutes les 2 secondes
    function surSaisie(e) {
        setTexte(e.target.value)
        const maintenant = Date.now()
        if (e.target.value && maintenant - dernierSignal.current > 2000) {
            dernierSignal.current = maintenant
            onSaisie()
        }
    }

    function soumettre(e) {
        e.preventDefault()
        const contenu = texte.trim()
        if (!contenu) return
        if (onEnvoyer(contenu)) {
            setTexte('')
        }
    }

    return (
        <>
            <header className="chat-header">
                <strong>{contact.username}</strong>
                <span className="chat-statut">{contact.connecte ? 'en ligne' : 'hors ligne'}</span>
                {ecrit && <span className="chat-ecrit">est en train d'écrire...</span>}
            </header>

            <div className="messages-list">
                {messages.length === 0 && !erreur && (
                    <p className="aucun-message">Aucun message pour le moment.</p>
                )}
                {messages.map((m) => {
                    const mien = m.expediteurId === session.id
                    return (
                        <div key={m.id} className={mien ? 'message mien' : 'message autre'}>
                            <p>{m.contenu}</p>
                            <span className="message-heure">{formaterHeure(m.dateEnvoi)}</span>
                        </div>
                    )
                })}
                <div ref={bas} />
            </div>

            {erreur && <p className="erreur erreur-conversation">{erreur}</p>}

            <form className="chat-form" onSubmit={soumettre}>
                <input
                    className="chat-input"
                    type="text"
                    placeholder={canalOuvert ? 'Écris un message...' : 'Connexion au canal en cours...'}
                    value={texte}
                    onChange={surSaisie}
                    maxLength={2000}
                    disabled={!canalOuvert}
                    autoFocus
                />
                <button className="chat-submit" type="submit" disabled={!canalOuvert || !texte.trim()}>
                    Envoyer
                </button>
            </form>
        </>
    )
}