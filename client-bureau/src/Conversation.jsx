import { useState, useEffect, useRef } from 'react'
import EmojiPicker, { EmojiStyle, Categories } from 'emoji-picker-react'
import emojisFr from 'emoji-picker-react/dist/data/emojis-fr'

// Noms des categories du selecteur d'emojis, en francais
const CATEGORIES_EMOJI = [
    { category: Categories.SUGGESTED, name: 'Récents' },
    { category: Categories.SMILEYS_PEOPLE, name: 'Émoticônes et personnes' },
    { category: Categories.ANIMALS_NATURE, name: 'Animaux et nature' },
    { category: Categories.FOOD_DRINK, name: 'Nourriture et boissons' },
    { category: Categories.TRAVEL_PLACES, name: 'Voyages et lieux' },
    { category: Categories.ACTIVITIES, name: 'Activités' },
    { category: Categories.OBJECTS, name: 'Objets' },
    { category: Categories.SYMBOLS, name: 'Symboles' },
    { category: Categories.FLAGS, name: 'Drapeaux' },
]

function formaterHeure(dateIso) {
    return new Date(dateIso).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })
}

export default function Conversation({ session, contact, messages, erreur, canalOuvert, ecrit, onEnvoyer, onSaisie }) {
    const [texte, setTexte] = useState('')
    const bas = useRef(null)
    const dernierSignal = useRef(0)
    const champ = useRef(null)
    const zoneEmoji = useRef(null)
    const [emojisOuverts, setEmojisOuverts] = useState(false)

    // Descend en bas de la liste quand les messages changent
    useEffect(() => {
        bas.current?.scrollIntoView()
    }, [messages])

    // Ferme le selecteur d'emojis quand on clique ailleurs
    useEffect(() => {
        if (!emojisOuverts) return undefined
        function clicDehors(e) {
            if (zoneEmoji.current && !zoneEmoji.current.contains(e.target)) {
                setEmojisOuverts(false)
            }
        }
        document.addEventListener('mousedown', clicDehors)
        return () => document.removeEventListener('mousedown', clicDehors)
    }, [emojisOuverts])

    // Met a jour le texte et signale la saisie au serveur, au plus une fois toutes les 2 secondes
    function changerTexte(valeur) {
        setTexte(valeur)
        const maintenant = Date.now()
        if (valeur && maintenant - dernierSignal.current > 2000) {
            dernierSignal.current = maintenant
            onSaisie()
        }
    }

    function surSaisie(e) {
        changerTexte(e.target.value)
    }

    // Insere l'emoji choisi a l'endroit du curseur, comme s'il avait ete tape
    // (on lit le texte dans le champ lui-meme : le selecteur garde parfois une ancienne version de cette fonction)
    function surEmoji(emojiData) {
        const emoji = emojiData.emoji
        const input = champ.current
        if (!input) return
        const actuel = input.value
        if (actuel.length + emoji.length > 2000) return
        const debut = input.selectionStart ?? actuel.length
        const fin = input.selectionEnd ?? actuel.length
        changerTexte(actuel.slice(0, debut) + emoji + actuel.slice(fin))
        // Replace le curseur juste apres l'emoji
        requestAnimationFrame(() => {
            input.focus()
            input.setSelectionRange(debut + emoji.length, debut + emoji.length)
        })
    }

    function soumettre(e) {
        e.preventDefault()
        const contenu = texte.trim()
        if (!contenu) return
        if (onEnvoyer(contenu)) {
            setTexte('')
            setEmojisOuverts(false)
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
                <div className="emoji-zone" ref={zoneEmoji}>
                    <button
                        type="button"
                        className="emoji-bouton"
                        aria-label="Choisir un emoji"
                        aria-expanded={emojisOuverts}
                        disabled={!canalOuvert}
                        onClick={() => setEmojisOuverts((ouvert) => !ouvert)}
                    >
                        😊
                    </button>
                    {emojisOuverts && (
                        <div className="emoji-panneau">
                            <EmojiPicker
                                onEmojiClick={surEmoji}
                                emojiData={emojisFr}
                                emojiStyle={EmojiStyle.NATIVE}
                                categories={CATEGORIES_EMOJI}
                                searchPlaceholder="Rechercher"
                                previewConfig={{ showPreview: false }}
                                lazyLoadEmojis
                                width={340}
                                height={380}
                            />
                        </div>
                    )}
                </div>
                <input
                    ref={champ}
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