
// Le serveur Spring Boot tourne sur le port 8080 de la machine qui l'heberge.
// On reprend le nom d'hote de la page : en local c'est "localhost", et le jour
// de la demo, si la page est ouverte via http://192.168.x.x:5173, le client
// ira automatiquement chercher le serveur sur 192.168.x.x.
const HOTE = window.location.hostname

export const API_URL = `http://${HOTE}:8080`
export const WS_URL = `ws://${HOTE}:8080/ws/messages`