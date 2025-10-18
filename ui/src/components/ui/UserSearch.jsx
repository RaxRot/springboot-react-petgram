import { useState } from "react"
import { toast } from "sonner"
import { useNavigate } from "react-router-dom"
import { api } from "@/lib/axios"
import { cn } from "@/lib/cn"

export default function UserSearch() {
    const [q, setQ] = useState("")
    const [isLoading, setIsLoading] = useState(false)
    const [shake, setShake] = useState(false)
    const nav = useNavigate()

    const onSubmit = async (e) => {
        e.preventDefault()
        const username = q.trim()
        if (!username) return

        setIsLoading(true)
        try {
            await api.get(`/api/public/users/${encodeURIComponent(username)}`)
            nav(`/profile/${encodeURIComponent(username)}`)
            setQ("")
        } catch {
            toast.error("👤 User not found")
            setShake(true)
            setTimeout(() => setShake(false), 500)
        } finally {
            setIsLoading(false)
        }
    }

    return (
        <form
            onSubmit={onSubmit}
            className="relative flex items-center gap-3 group transition-all"
        >
            <div
                className={cn(
                    "relative rounded-2xl overflow-hidden transition-all duration-500 ease-out",
                    // ФИКС: убрал хардкод, используем CSS переменные
                    "border border-border",
                    "bg-background/80 backdrop-blur-md",
                    "hover:border-primary/40 hover:shadow-[0_0_10px_rgba(56,189,248,0.25)]",
                    "focus-within:border-primary focus-within:shadow-[0_0_15px_rgba(56,189,248,0.4)]",
                    shake && "animate-shake"
                )}
            >
                {/* Search icon */}
                <div className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground">
                    🔍
                </div>

                <input
                    value={q}
                    onChange={(e) => setQ(e.target.value)}
                    placeholder="Search @username…"
                    disabled={isLoading}
                    className={cn(
                        "w-60 md:w-64 group-focus-within:w-72 transition-all duration-500",
                        "pl-10 pr-10 py-2.5 text-sm rounded-2xl outline-none bg-transparent",
                        // ФИКС: используем CSS переменные для текста
                        "text-foreground placeholder:text-muted-foreground",
                        "disabled:opacity-50 disabled:cursor-not-allowed"
                    )}
                />

                {/* Loading spinner */}
                {isLoading && (
                    <div className="absolute right-3 top-1/2 -translate-y-1/2">
                        <div className="w-4 h-4 border-2 border-primary/30 border-t-primary rounded-full animate-spin"></div>
                    </div>
                )}
            </div>

            {/* Neon button */}
            <button
                type="submit"
                disabled={isLoading || !q.trim()}
                className={cn(
                    "relative h-10 px-5 rounded-xl font-semibold transition-all duration-300 ease-out",
                    "bg-gradient-to-r from-cyan-400 to-purple-600 text-white",
                    "hover:from-cyan-500 hover:to-purple-700",
                    "hover:shadow-lg hover:shadow-cyan-500/30",
                    "hover:scale-105 active:scale-95",
                    "disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:scale-100",
                    "overflow-hidden"
                )}
            >
                {isLoading ? (
                    <div className="flex items-center">
                        <div className="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                        <span className="ml-2">Searching...</span>
                    </div>
                ) : (
                    <span className="relative z-10">Search</span>
                )}
                <div className="absolute inset-0 bg-gradient-to-r from-cyan-400/20 to-purple-600/20 blur-md opacity-0 hover:opacity-100 transition-opacity duration-500"></div>
            </button>
        </form>
    )
}