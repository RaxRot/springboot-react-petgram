import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link } from "react-router-dom"
import Skeleton from "@/components/ui/Skeleton"

export default function MessagesList() {
    const q = useQuery({
        queryKey: ["dialogs"],
        queryFn: async () => (await api.get("/api/chat/dialogs")).data
    })

    if (q.isLoading)
        return (
            <div className="max-w-2xl mx-auto p-6 space-y-3">
                {[...Array(4)].map((_, i) => (
                    <div
                        key={i}
                        className="p-4 rounded-2xl bg-[hsl(var(--card))]/80 border border-[hsl(var(--border))] shadow-[0_0_20px_rgba(56,189,248,0.08)]"
                    >
                        <Skeleton className="h-5 w-1/3 mb-2" />
                        <Skeleton className="h-4 w-2/3" />
                    </div>
                ))}
            </div>
        )

    if (q.error) return <p className="text-red-500">{q.error.message}</p>

    return (
        <div className="max-w-2xl mx-auto p-6 space-y-6">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                Messages
            </h1>

            {/* 📨 No dialogs */}
            {(!q.data || q.data.length === 0) ? (
                <div className="text-center py-16 rounded-2xl bg-[hsl(var(--card))]/70 border border-[hsl(var(--border))] shadow-[0_0_25px_hsl(var(--ring))]/20 backdrop-blur-xl">
                    <div className="text-6xl mb-4">💬</div>
                    <p className="text-[hsl(var(--foreground))] text-lg font-medium">No conversations yet</p>
                    <p className="text-[hsl(var(--muted-foreground))] text-sm mt-1">
                        Start chatting with other pet lovers!
                    </p>
                </div>
            ) : (
                <div className="grid gap-3">
                    {q.data.map(d => (
                        <Link
                            key={d.peerId}
                            to={`/messages/${encodeURIComponent(d.peerUsername)}`}
                            className="block rounded-2xl p-4 border border-[hsl(var(--border))]
                                     bg-[hsl(var(--card))]/80 backdrop-blur-xl
                                     hover:border-cyan-400/40 hover:shadow-[0_0_25px_rgba(56,189,248,0.25)]
                                     transition-all duration-300"
                        >
                            <div className="font-semibold text-[hsl(var(--foreground))]">@{d.peerUsername}</div>
                            <div className="text-sm text-[hsl(var(--muted-foreground))] line-clamp-1">
                                {d.lastMessage}
                            </div>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    )
}
