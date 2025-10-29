import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Skeleton from "@/components/ui/Skeleton"
import { toast } from "sonner"

export default function AdminInsights() {
    const { data, isLoading, isError } = useQuery({
        queryKey: ["adminInsights"],
        queryFn: async () => (await api.get("/api/admin/users/insights")).data,
    })

    if (isLoading)
        return (
            <div className="max-w-4xl mx-auto p-6 space-y-4">
                <h2 className="text-2xl font-bold text-cyan-400 flex items-center gap-2">
                    📈 Loading insights...
                </h2>
                {[...Array(5)].map((_, i) => (
                    <Skeleton key={i} className="h-20 w-full rounded-2xl" variant="card" />
                ))}
            </div>
        )

    if (isError) {
        toast.error("Failed to load insights")
        return (
            <div className="max-w-4xl mx-auto p-6">
                <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                    Error loading insights 😿
                </div>
            </div>
        )
    }

    return (
        <div className="relative max-w-5xl mx-auto p-6 space-y-8">
            {/* Header */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    🧠 Admin Insights
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] mt-2">
                    Key community metrics at a glance
                </p>
            </div>

            {/* Cards */}
            <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
                {Object.entries(data || {}).map(([key, value], index) => (
                    <div
                        key={key}
                        className="p-6 rounded-3xl border border-[hsl(var(--border))]
              bg-[hsl(var(--card))]/90 backdrop-blur-xl
              shadow-[0_0_25px_rgba(56,189,248,0.12)]
              hover:shadow-[0_0_40px_rgba(56,189,248,0.25)]
              transition-all duration-300 text-center group"
                    >
                        <div className="text-3xl mb-3">
                            {["💖", "💬", "💰", "🐶", "🔥", "👑"][index % 6]}
                        </div>
                        <h3 className="text-lg font-semibold text-cyan-400 mb-1 capitalize">
                            {key.replace(/([A-Z])/g, " $1")}
                        </h3>
                        <p className="text-[hsl(var(--foreground))] font-medium text-sm">
                            {String(value)}
                        </p>
                    </div>
                ))}
            </div>

            {/* Background glow */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    )
}
