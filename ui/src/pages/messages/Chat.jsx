import { useEffect, useMemo, useRef, useState } from "react"
import { Link, useParams } from "react-router-dom"
import { useMutation, useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import Input from "@/components/ui/Input"
import { useAuth } from "@/store/auth"

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
                const { data } = await api.get(`/api/public/users/username/${encodeURIComponent(username)}/id`)
                const id = typeof data === "number" ? data : data?.id
                if (!cancelled) setPeerId(id)
            } catch {
                // ignore
            }
        })()
        return () => { cancelled = true }
    }, [username])

    // load history
    const convoQ = useQuery({
        enabled: !!peerId,
        queryKey: ["chat", peerId],
        queryFn: async () => (await api.get(`/api/chat/${peerId}/messages?page=0&size=50`)).data,
        onSuccess: (data) => setMessages(data.content ?? []),
    })

    // polling new
    useEffect(() => {
        if (!peerId) return
        const t = setInterval(async () => {
            try {
                const { data } = await api.get(`/api/chat/${peerId}/new`, { params: { afterId: lastId } })
                if (Array.isArray(data) && data.length) {
                    setMessages(prev => [...prev, ...data])
                }
            } catch {}
        }, 2000)
        return () => clearInterval(t)
    }, [peerId, lastId])

    // autoscroll to bottom
    useEffect(() => {
        scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" })
    }, [messages.length])

    const send = useMutation({
        mutationFn: async () => (await api.post(`/api/chat/${peerId}/messages`, { text })).data,
        onSuccess: (msg) => {
            setText("")
            setMessages(prev => [...prev, msg])
        },
    })

    if (!peerId) return <p>Loading chat…</p>
    if (convoQ.isLoading) return <p>Loading messages…</p>

    return (
        <div className="max-w-3xl mx-auto space-y-3">
            <div className="flex items-center justify-between">
                <h1 className="text-xl font-semibold">@{username}</h1>
                <Link to={`/u/${username}`} className="opacity-70 hover:opacity-100 text-sm">View profile</Link>
            </div>

            {/* messages area */}
            <div
                ref={scrollRef}
                className="h-[60vh] rounded-2xl border p-4 overflow-y-auto bg-white"
            >
                {messages.map(m => {
                    const mine = m.senderId === user?.id
                    return (
                        <div key={m.id} className={`mb-3 flex ${mine ? "justify-end" : "justify-start"}`}>
                            <div
                                className={`
                  max-w-[75%] rounded-2xl px-3 py-2
                  ${mine ? "bg-[--color-primary]/10" : "bg-[--color-muted]"}
                  text-slate-900
                `}
                            >
                                <div className="whitespace-pre-wrap break-words leading-snug">{m.text}</div>
                                <div className="text-[11px] text-slate-500 mt-1">
                                    {new Date(m.createdAt).toLocaleString()}
                                </div>
                            </div>
                        </div>
                    )
                })}
            </div>

            {/* input */}
            <form
                onSubmit={(e) => {
                    e.preventDefault()
                    if (!text.trim() || send.isPending) return
                    send.mutate()
                }}
                className="flex items-center gap-2"
            >
                <Input
                    placeholder="Type a message…"
                    value={text}
                    onChange={(e) => setText(e.target.value)}
                    className="flex-1"
                />
                <Button type="submit" disabled={send.isPending}>Send</Button>
            </form>
        </div>
    )
}
