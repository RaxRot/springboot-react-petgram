import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link } from "react-router-dom"
import Skeleton from "@/components/ui/Skeleton"
import ResponsiveImage from "@/components/ui/ResponsiveImage"

export default function Bookmarks() {
    const q = useQuery({
        queryKey: ["bookmarks"],
        queryFn: async () =>
            (await api.get("/api/user/bookmarks?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc")).data,
    })

    if (q.isLoading)
        return (
            <div className="max-w-7xl mx-auto p-6 space-y-6">
                <Skeleton className="h-10 w-64" variant="text" />
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {[...Array(8)].map((_, i) => (
                        <div
                            key={i}
                            className="p-4 rounded-2xl border border-[hsl(var(--border))]
                                bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                                backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]"
                        >
                            <Skeleton className="h-4 w-20 mb-3" variant="text" />
                            <Skeleton className="h-6 w-full mb-4" variant="text" />
                            <Skeleton className="h-48 w-full rounded-xl" variant="image" />
                        </div>
                    ))}
                </div>
            </div>
        )

    if (q.error)
        return (
            <div className="max-w-7xl mx-auto p-6">
                <div className="border border-[hsl(var(--destructive))]/30 bg-[hsl(var(--destructive))]/10
                    text-[hsl(var(--destructive-foreground))] rounded-2xl p-4">
                    Error: {q.error.message}
                </div>
            </div>
        )

    return (
        <div className="relative max-w-7xl mx-auto p-6 space-y-6">
            {/* Header */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    My Bookmarks
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] mt-2">
                    Your saved pet moments
                </p>
            </div>

            {/* Empty */}
            {q.data.content?.length === 0 ? (
                <div className="text-center py-16">
                    <div className="text-6xl mb-4">🔖</div>
                    <p className="text-[hsl(var(--muted-foreground))] text-lg">
                        No bookmarks yet
                    </p>
                    <p className="text-[hsl(var(--muted-foreground))] text-sm">
                        Start saving your favorite posts!
                    </p>
                </div>
            ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {q.data.content?.map((p) => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="group relative block p-4 rounded-2xl border border-[hsl(var(--border))]
                                bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                                backdrop-blur-xl hover:shadow-[0_0_25px_hsl(var(--ring))]
                                hover:border-[hsl(var(--ring))]/50 transition-all duration-300"
                        >
                            {/* Animal type badge */}
                            <div className="text-xs font-semibold text-[hsl(var(--ring))] mb-2 uppercase">
                                {p.animalType}
                            </div>

                            {/* Title */}
                            <h3 className="text-lg font-semibold line-clamp-2 group-hover:text-[hsl(var(--ring))] transition-colors">
                                {p.title}
                            </h3>

                            {/* Author */}
                            <div className="text-sm text-[hsl(var(--muted-foreground))] mt-2">
                                by @{p.user?.userName ?? "unknown"}
                            </div>

                            {/* Image */}
                            {p.imageUrl && (
                                <div className="mt-3 rounded-xl overflow-hidden">
                                    <ResponsiveImage
                                        src={p.imageUrl}
                                        alt={p.title}
                                        className="group-hover:scale-105 transition-transform duration-300"
                                    />
                                </div>
                            )}

                            {/* Bookmark icon */}
                            <div className="absolute top-3 right-3 w-8 h-8 rounded-full
                                bg-gradient-to-r from-cyan-400 to-purple-600
                                flex items-center justify-center text-white text-sm
                                opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                                🔖
                            </div>
                        </Link>
                    ))}
                </div>
            )}

            {/* Background Glow */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    )
}
