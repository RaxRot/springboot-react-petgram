import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link } from "react-router-dom"

export default function MessagesList() {
    const q = useQuery({
        queryKey: ["dialogs"],
        queryFn: async () => (await api.get("/api/chat/dialogs")).data
    })

    if (q.isLoading) return <p>Loading dialogs…</p>
    if (q.error) return <p className="text-red-500">{q.error.message}</p>

    return (
        <div className="space-y-4">
            <h1 className="text-2xl font-bold">Messages</h1>
            {(!q.data || q.data.length === 0) && <p>No conversations yet.</p>}

            <div className="grid gap-3">
                {q.data?.map(d => (
                    <Link
                        key={d.peerId}
                        to={`/messages/${encodeURIComponent(d.peerUsername)}`}
                        className="rounded-xl border p-4 hover:bg-[--color-muted] transition"
                    >
                        <div className="font-semibold">@{d.peerUsername}</div>
                        <div className="text-sm opacity-70 line-clamp-1">{d.lastMessage}</div>
                    </Link>
                ))}
            </div>
        </div>
    )
}
