import SockJS from "sockjs-client"
import { Client } from "@stomp/stompjs"

let client = null
export function connectWS(onConnect) {
    if (client?.active) return client
    client = new Client({
        webSocketFactory: () => new SockJS("http://localhost:8080/ws"),
        reconnectDelay: 3000,
    })
    client.onConnect = onConnect
    client.activate()
    return client
}
export function getClient() { return client }
