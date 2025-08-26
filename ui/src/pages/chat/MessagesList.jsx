import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link } from "react-router-dom"

export default function MessagesList() {
    const q = useQuery({
        queryKey: ["dialogs"],
        queryFn: async () => (await api.get("/api/chat/dialogs")).data
    })

    if (q.isLoading) return <p>Loading…</p>
    if (q.error) return <p className="text-red-500">{q.error.message}</p>

    return (
        <div className="max-w-2xl mx-auto space-y-3">
            <h1 className="text-2xl font-bold mb-2">Messages</h1>
            {q.data.length === 0 && <p>No dialogs yet.</p>}
            {q.data.map(d => (
                <Link
                    key={d.username}
                    to={`/chat/${d.username}`}
                    className="block border rounded-xl p-3 hover:bg-[--color-muted] transition"
                >
                    <div className="font-semibold">@{d.username}</div>
                    <div className="text-sm opacity-70">{d.lastMessage}</div>
                </Link>
            ))}
        </div>
    )
}
