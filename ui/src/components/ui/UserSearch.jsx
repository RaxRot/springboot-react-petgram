import { useState } from "react"
import { toast } from "sonner"
import { useNavigate } from "react-router-dom"
import { api } from "@/lib/axios"
import { cn } from "@/lib/cn"

export default function UserSearch() {
    const [q, setQ] = useState("")
    const [isLoading, setIsLoading] = useState(false)
    const nav = useNavigate()

    const onSubmit = async (e) => {
        e.preventDefault()
        const username = q.trim()
        if (!username) return

        setIsLoading(true)
        try {
            // проверим, что такой пользователь есть
            await api.get(`/api/public/users/${encodeURIComponent(username)}`)
            nav(`/profile/${encodeURIComponent(username)}`)
            setQ("") // Clear input after successful search
        } catch {
            toast.error("👤 User not found")
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <form
            onSubmit={onSubmit}
            className="relative flex items-center gap-2 group"
        >
            <div className="relative">
                {/* Search icon */}
                <div className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 group-focus-within:text-cyan-400 transition-colors duration-300">
                    🔍
                </div>

                <input
                    value={q}
                    onChange={(e) => setQ(e.target.value)}
                    placeholder="Search @username…"
                    disabled={isLoading}
                    className={cn(
                        "h-10 pl-10 pr-4 rounded-xl border-0 outline-none transition-all duration-300 ease-out",
                        "bg-white/10 backdrop-blur-sm text-white placeholder:text-gray-400",
                        "focus:bg-white/15 focus:ring-2 focus:ring-cyan-400/30",
                        "focus:shadow-lg focus:shadow-cyan-500/20",
                        "hover:bg-white/12 border border-white/20 hover:border-white/30",
                        "disabled:opacity-50 disabled:cursor-not-allowed",
                        "w-64 group-hover:w-72 group-focus-within:w-72 transition-width duration-500"
                    )}
                />

                {/* Loading indicator */}
                {isLoading && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                        <div className="w-4 h-4 border-2 border-cyan-400/30 border-t-cyan-400 rounded-full animate-spin"></div>
                    </div>
                )}
            </div>

            <button
                type="submit"
                disabled={isLoading || !q.trim()}
                className={cn(
                    "h-10 px-4 rounded-xl border-0 font-semibold transition-all duration-300 ease-out",
                    "bg-gradient-to-r from-cyan-400 to-purple-600 text-white",
                    "hover:from-cyan-500 hover:to-purple-700",
                    "hover:shadow-lg hover:shadow-cyan-500/30",
                    "hover:scale-105 active:scale-95",
                    "disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:scale-100",
                    "flex items-center gap-2"
                )}
            >
                {isLoading ? (
                    <>
                        <div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                        Searching...
                    </>
                ) : (
                    "Search"
                )}
            </button>
        </form>
    )
}