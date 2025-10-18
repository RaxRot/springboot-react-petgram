import { useEffect, useMemo, useRef, useState } from "react"
import { Link, useParams } from "react-router-dom"
import { useMutation, useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import Input from "@/components/ui/Input"
import { useAuth } from "@/store/auth"
import Skeleton from "@/components/ui/Skeleton"

export default function Chat() {
    const { username } = useParams()
    const { user } = useAuth()

    const [peerId, setPeerId] = useState(null)
    const [text, setText] = useState("")
    const [messages, setMessages] = useState([])

    const scrollRef = useRef(null)
    const lastId = useMemo(() => messages.at(-1)?.id ?? 0, [messages])

    // resolve peer id
    useEffect(() => {
        let cancelled = false
        ;(async () => {
            try {
                const { data } = await api.get(
                    `/api/public/users/username/${encodeURIComponent(username)}/id`
                )
                const id = typeof data === "number" ? data : data?.id
                if (!cancelled) setPeerId(id)
            } catch {
                // ignore
            }
        })()
        return () => {
            cancelled = true
        }
    }, [username])

    // load history
    const convoQ = useQuery({
        enabled: !!peerId,
        queryKey: ["chat", peerId],
        queryFn: async () =>
            (await api.get(`/api/chat/${peerId}/messages?page=0&size=50`)).data,
        onSuccess: (data) => setMessages(data.content ?? []),
    })

    // polling new
    useEffect(() => {
        if (!peerId) return
        const t = setInterval(async () => {
            try {
                const { data } = await api.get(`/api/chat/${peerId}/new`, {
                    params: { afterId: lastId },
                })
                if (Array.isArray(data) && data.length) {
                    setMessages((prev) => [...prev, ...data])
                }
            } catch {}
        }, 2000)
        return () => clearInterval(t)
    }, [peerId, lastId])

    // autoscroll to bottom
    useEffect(() => {
        scrollRef.current?.scrollTo({
            top: scrollRef.current.scrollHeight,
            behavior: "smooth",
        })
    }, [messages.length])

    const send = useMutation({
        mutationFn: async () =>
            (await api.post(`/api/chat/${peerId}/messages`, { text })).data,
        onSuccess: (msg) => {
            setText("")
            setMessages((prev) => [...prev, msg])
        },
    })

    if (!username) {
        return (
            <div className="max-w-3xl mx-auto p-6 text-center text-[hsl(var(--muted-foreground))]">
                Please select a conversation
            </div>
        )
    }

    if (!peerId || convoQ.isLoading) {
        return (
            <div className="max-w-3xl mx-auto p-6 space-y-4">
                <Skeleton className="h-8 w-48" variant="text" />
                <Skeleton className="h-[60vh] w-full rounded-2xl" variant="card" />
                <div className="flex gap-2">
                    <Skeleton className="flex-1 h-12 rounded-xl" variant="card" />
                    <Skeleton className="w-20 h-12 rounded-xl" variant="card" />
                </div>
            </div>
        )
    }

    return (
        <div className="max-w-3xl mx-auto p-6 space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    @{username}
                </h1>
                <Link
                    to={`/u/${username}`}
                    className="text-cyan-600 dark:text-cyan-400 hover:text-cyan-700 dark:hover:text-cyan-300 text-sm underline underline-offset-4 transition-colors duration-300"
                >
                    View profile
                </Link>
            </div>

            {/* Messages area */}
            <div
                ref={scrollRef}
                className="h-[60vh] rounded-2xl border border-[hsl(var(--border))]
                           bg-[hsl(var(--card))]/80 backdrop-blur-xl
                           p-4 overflow-y-auto scrollbar-thin
                           shadow-[0_0_25px_rgba(56,189,248,0.08)]
                           transition-all duration-300"
            >
                {messages.length === 0 ? (
                    <div className="h-full flex items-center justify-center">
                        <div className="text-center text-[hsl(var(--muted-foreground))]">
                            <div className="text-4xl mb-2">💬</div>
                            <p>No messages yet</p>
                            <p className="text-sm">Start the conversation!</p>
                        </div>
                    </div>
                ) : (
                    messages.map((m) => {
                        const mine = m.senderId === user?.id
                        return (
                            <div
                                key={m.id}
                                className={`mb-4 flex ${mine ? "justify-end" : "justify-start"}`}
                            >
                                <div
                                    className={`
                                        max-w-[75%] rounded-2xl px-4 py-3 relative
                                        transition-all duration-300
                                        ${
                                        mine
                                            ? "bg-gradient-to-r from-cyan-400 to-purple-600 text-white shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50"
                                            : "bg-[hsl(var(--card))]/70 border border-[hsl(var(--border))] text-[hsl(var(--foreground))] hover:border-cyan-400/40"
                                    }
                                    `}
                                >
                                    <div className="whitespace-pre-wrap break-words leading-snug">
                                        {m.text}
                                    </div>
                                    <div
                                        className={`text-xs mt-2 ${
                                            mine
                                                ? "text-cyan-100/70"
                                                : "text-[hsl(var(--muted-foreground))]"
                                        }`}
                                    >
                                        {new Date(m.createdAt).toLocaleTimeString()}
                                    </div>

                                    {/* Message tail */}
                                    <div
                                        className={`absolute top-3 w-3 h-3 ${
                                            mine ? "-right-2" : "-left-2"
                                        } transform rotate-45 ${
                                            mine
                                                ? "bg-gradient-to-r from-cyan-400 to-purple-600"
                                                : "bg-[hsl(var(--card))]/70 border-l border-b border-[hsl(var(--border))]"
                                        }`}
                                    />
                                </div>
                            </div>
                        )
                    })
                )}
            </div>

            {/* Input form */}
            <form
                onSubmit={(e) => {
                    e.preventDefault()
                    if (!text.trim() || send.isPending) return
                    send.mutate()
                }}
                className="flex items-center gap-3"
            >
                <Input
                    placeholder="Type a message…"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    className="flex-1"
                />
                <Button
                    type="submit"
                    disabled={send.isPending || !text.trim()}
                    className="px-6 bg-gradient-to-r from-cyan-400 to-purple-600"
                >
                    {send.isPending ? (
                        <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                    ) : (
                        "Send"
                    )}
                </Button>
            </form>
        </div>
    )
}
