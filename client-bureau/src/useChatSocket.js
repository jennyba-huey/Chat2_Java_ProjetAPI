import { useEffect, useRef, useState, useCallback } from 'react'
import { WS_URL } from './config'

// Ouvre le canal WebSocket du serveur avec le jeton JWT, et le referme
// quand l'utilisateur se deconnecte. Chaque message recu (JSON) est confie
// a la fonction surEvenement.
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

        const socket = new WebSocket(`${WS_URL}?token=${encodeURIComponent(token)}`)
        socketRef.current = socket

        socket.onopen = () => {
            setConnecte(true)
            gestionnaire.current({ type: 'ouverture' })
        }
        socket.onclose = () => {
            // Ignore la fermeture d'un ancien canal deja remplace
            if (socketRef.current === socket) setConnecte(false)
        }
        socket.onmessage = (e) => {
            try {
                gestionnaire.current(JSON.parse(e.data))
            } catch {
                // Message non JSON : ignore
            }
        }

        return () => {
            socket.close()
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