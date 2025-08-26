import { useState } from "react"
import { toast } from "sonner"
import { useNavigate } from "react-router-dom"
import { api } from "@/lib/axios"

export default function UserSearch() {
    const [q, setQ] = useState("")
    const nav = useNavigate()

    const onSubmit = async (e) => {
        e.preventDefault()
        const username = q.trim()
        if (!username) return

        try {
            // проверим, что такой пользователь есть
            await api.get(`/api/public/users/${encodeURIComponent(username)}`)
            nav(`/profile/${encodeURIComponent(username)}`)
        } catch {
            toast.error("User not found")
        }
    }

    return (
        <form onSubmit={onSubmit} className="flex items-center gap-2">
            <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Search @username…"
                className="h-9 rounded-lg border px-3 text-sm outline-none focus:ring-2 focus:ring-black/10"
            />
            <button className="h-9 rounded-lg px-3 text-sm border hover:bg-black/5" type="submit">
                Search
            </button>
        </form>
    )
}
