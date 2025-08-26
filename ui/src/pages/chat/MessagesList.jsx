import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link } from "react-router-dom"
import Skeleton from "@/components/ui/Skeleton"

export default function MessagesList() {
    const q = useQuery({
        queryKey: ["dialogs"],
        queryFn: async () => (await api.get("/api/chat/dialogs")).data
    })

    if (q.isLoading) return <p>Loading dialogs…</p>
    if (q.error) return <p className="text-red-500">{q.error.message}</p>

    return (
        <div className="max-w-2xl mx-auto p-6 space-y-4">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                Messages
            </h1>

            {(!q.data || q.data.length === 0) ? (
                <div className="text-center py-12">
                    <div className="text-6xl mb-4">💬</div>
                    <p className="text-gray-600 dark:text-gray-400 text-lg">No conversations yet</p>
                    <p className="text-gray-500 dark:text-gray-500 text-sm">Start chatting with other pet lovers!</p>
                </div>
            ) : (
                <div className="grid gap-3">
                    {q.data.map(d => (
                        <Link
                            key={d.peerId}
                            to={`/messages/${encodeURIComponent(d.peerUsername)}`}
                            className="block bg-gray-100 dark:bg-white/5 border border-gray-200 dark:border-white/10 rounded-2xl p-4
                                     hover:bg-gray-200 dark:hover:bg-white/10 hover:border-cyan-400/30 transition-all duration-300"
                        >
                            <div className="font-semibold text-gray-900 dark:text-white">@{d.peerUsername}</div>
                            <div className="text-sm text-gray-600 dark:text-gray-400 line-clamp-1">{d.lastMessage}</div>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    )
}