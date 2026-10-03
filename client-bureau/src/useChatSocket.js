import { useEffect, useRef, useState, useCallback } from 'react'
import { WS_URL } from './config'

// Delai maximum entre deux tentatives de reconnexion
const DELAI_MAX = 10000

// Ouvre le canal WebSocket du serveur avec le jeton JWT et le rouvre
// automatiquement s'il se ferme (serveur redemarre, reseau coupe).
// Chaque message recu (JSON) est confie a la fonction surEvenement.
export default function useChatSocket(token, surEvenement) {
    const socketRef = useRef(null)
    const gestionnaire = useRef(surEvenement)
    const [connecte, setConnecte] = useState(false)

    // Garde la derniere version du gestionnaire sans rouvrir le canal
    useEffect(() => {
        gestionnaire.current = surEvenement
    })

    useEffect(() => {
        if (!token) return undefined

        let actif = true
        let tentative = 0
        let minuteur = null

        function ouvrir() {
            const socket = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`)
            socketRef.current = socket

            socket.onopen = () => {
                tentative = 0
                setConnecte(true)
                gestionnaire.current({ type: 'ouverture' })
            }

            socket.onclose = () => {
                // Ignore la fermeture d'un ancien canal deja remplace
                if (socketRef.current !== socket) return
                setConnecte(false)
                if (!actif) return
                // Attente de plus en plus longue entre deux tentatives
                const delai = Math.min(1000 * 2 ** tentative, DELAI_MAX)
                tentative += 1
                minuteur = setTimeout(ouvrir, delai)
            }

            socket.onmessage = (e) => {
                try {
                    gestionnaire.current(JSON.parse(e.data))
                } catch {
                    // Message non JSON : ignore
                }
            }
        }

        ouvrir()

        return () => {
            actif = false
            clearTimeout(minuteur)
            socketRef.current?.close()
        }
    }, [token])

    const envoyer = useCallback((objet) => {
        const socket = socketRef.current
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(objet))
            return true
        }
        return false
    }, [])

    return { connecte, envoyer }
}