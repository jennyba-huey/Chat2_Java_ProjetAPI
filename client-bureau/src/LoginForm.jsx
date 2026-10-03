

import { useState } from 'react'
import { inscrire, connecter } from './api'

export default function LoginForm({ onConnecte }) {
    const [modeInscription, setModeInscription] = useState(false)
    const [username, setUsername] = useState('')
    const [motDePasse, setMotDePasse] = useState('')
    const [erreur, setErreur] = useState('')
    const [enCours, setEnCours] = useState(false)

    async function soumettre(e) {
        e.preventDefault()
        setErreur('')
        setEnCours(true)
        try {
            if (modeInscription) {
                await inscrire(username.trim(), motDePasse)
            }
            const session = await connecter(username.trim(), motDePasse)
            onConnecte({ token: session.token, id: session.id, username: session.username })
        } catch (err) {
            setErreur(err.message)
        } finally {
            setEnCours(false)
        }
    }

    return (
        <div className="login-container">
            <form className="login-form" onSubmit={soumettre}>
                <img src="/kawaiiMascot.svg" alt="" className="login-mascot" />
                <h1>{modeInscription ? 'Créer un compte' : 'Connexion'}</h1>

                <input
                    type="text"
                    placeholder="Nom d'utilisateur"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    minLength={3}
                    maxLength={50}
                    required
                    autoFocus
                />
                <input
                    type="password"
                    placeholder="Mot de passe (6 caractères minimum)"
                    value={motDePasse}
                    onChange={(e) => setMotDePasse(e.target.value)}
                    minLength={6}
                    required
                />

                {erreur && <p className="erreur">{erreur}</p>}

                <button type="submit" disabled={enCours}>
                    {enCours ? 'Patiente...' : modeInscription ? "S'inscrire" : 'Se connecter'}
                </button>

                <button
                    type="button"
                    className="lien"
                    onClick={() => {
                        setModeInscription(!modeInscription)
                        setErreur('')
                    }}
                >
                    {modeInscription ? 'Déjà un compte ? Se connecter' : 'Pas de compte ? Créer un compte'}
                </button>
            </form>
        </div>
    )
}