import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link, useSearchParams } from "react-router-dom"
import Button from "@/components/ui/Button"
import Skeleton from "@/components/ui/Skeleton"
import ResponsiveImage from "@/components/ui/ResponsiveImage"

const ANIMALS = ["ALL","DOG","CAT","BIRD","FISH","PIG","OTHER"]
const TABS = ["ALL","FOLLOWING"]

const ANIMAL_EMOJIS = {
    "ALL": "🌍", "DOG": "🐕", "CAT": "🐈", "BIRD": "🐦",
    "FISH": "🐠", "PIG": "🐷", "OTHER": "🐾"
}

export default function HomeFeed() {
    const [sp, setSp] = useSearchParams()
    const tab  = (sp.get("tab")  || "ALL").toUpperCase()
    const type = (sp.get("type") || "ALL").toUpperCase()

    const { data, isLoading, error } = useQuery({
        queryKey: ["feed", tab, type],
        queryFn: async () => {
            if (tab === "FOLLOWING") {
                const { data } = await api.get(
                    "/api/posts/feed/following?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc"
                )
                return data
            }
            if (type === "ALL") {
                const { data } = await api.get(
                    "/api/public/posts?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc"
                )
                return data
            } else {
                const { data } = await api.get(
                    `/api/public/posts/animal/${type}?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc`
                )
                return data
            }
        }
    })

    if (isLoading) return (
        <div className="max-w-7xl mx-auto p-6 space-y-6">
            {/* Tabs skeleton */}
            <div className="flex gap-2">
                {TABS.map(t => (
                    <Skeleton key={t} className="h-10 w-24 rounded-xl" variant="button"/>
                ))}
            </div>

            {/* Filters skeleton */}
            <div className="flex flex-wrap gap-2">
                {ANIMALS.map(a => (
                    <Skeleton key={a} className="h-10 w-20 rounded-xl" variant="button"/>
                ))}
            </div>

            {/* Posts grid skeleton */}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {[...Array(8)].map((_, i) => (
                    <div key={i} className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10">
                        <Skeleton className="h-4 w-20 mb-3" variant="text"/>
                        <Skeleton className="h-6 w-full mb-4" variant="text"/>
                        <Skeleton className="h-48 w-full rounded-xl" variant="image"/>
                    </div>
                ))}
            </div>
        </div>
    )

    if (error) return (
        <div className="max-w-7xl mx-auto p-6">
            <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                Error: {error.message}
            </div>
        </div>
    )

    const setTab = (t) => setSp(prev => {
        const p = new URLSearchParams(prev)
        p.set("tab", t)
        if (t === "FOLLOWING") p.delete("type")
        return p
    })

    const setType = (t) => setSp(prev => {
        const p = new URLSearchParams(prev)
        p.set("tab", "ALL")
        p.set("type", t)
        return p
    })

    return (
        <div className="max-w-7xl mx-auto p-6 space-y-6">
            {/* Header */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    PetSocial Feed
                </h1>
                <p className="text-gray-600 dark:text-gray-400 mt-2">Discover amazing pet moments</p>
            </div>

            {/* Tabs */}
            <div className="flex gap-2 justify-center">
                {TABS.map(t => (
                    <Button
                        key={t}
                        variant={t === tab ? "primary" : "outline"}
                        onClick={() => setTab(t)}
                        className="px-4 py-2 font-semibold"
                    >
                        {t === "FOLLOWING" ? "👥 Following" : "🌍 All Posts"}
                    </Button>
                ))}
            </div>

            {/* Animal filters */}
            {tab === "ALL" && (
                <div className="flex flex-wrap gap-2 justify-center">
                    {ANIMALS.map(a => (
                        <Button
                            key={a}
                            variant={a === type ? "primary" : "outline"}
                            onClick={() => setType(a)}
                            className="px-3 py-2 flex items-center gap-2"
                        >
                            <span>{ANIMAL_EMOJIS[a]}</span>
                            {a}
                        </Button>
                    ))}
                </div>
            )}

            {/* Posts grid */}
            {(!data?.content || data.content.length === 0) ? (
                <div className="text-center py-16">
                    <div className="text-6xl mb-4">
                        {tab === "FOLLOWING" ? "👥" : "📝"}
                    </div>
                    <p className="text-gray-600 dark:text-gray-400 text-lg">
                        {tab === "FOLLOWING"
                            ? "Your following feed is empty"
                            : "No posts yet"}
                    </p>
                    <p className="text-gray-500 dark:text-gray-500 text-sm">
                        {tab === "FOLLOWING"
                            ? "Follow someone to see their posts here"
                            : "Be the first to share a pet moment!"}
                    </p>
                </div>
            ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {data.content.map(p => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="block bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10
                                     hover:bg-gray-50 dark:hover:bg-white/10 hover:border-cyan-400/30 hover:shadow-lg hover:shadow-cyan-500/10
                                     transition-all duration-300 group"
                        >
                            {/* Animal type badge */}
                            <div className="text-xs font-semibold text-cyan-600 dark:text-cyan-400 mb-2 flex items-center gap-1">
                                <span>{ANIMAL_EMOJIS[p.animalType] || ANIMAL_EMOJIS.OTHER}</span>
                                {p.animalType}
                            </div>

                            {/* Title */}
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white line-clamp-2 group-hover:text-cyan-600 dark:group-hover:text-cyan-400 transition-colors">
                                {p.title}
                            </h3>

                            {/* Author */}
                            <div className="text-sm text-gray-600 dark:text-gray-400 mt-2">
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

                            {/* Hover overlay */}
                            <div className="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 group-hover:opacity-100 rounded-2xl transition-opacity duration-300" />
                        </Link>
                    ))}
                </div>
            )}

            {/* Background elements */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    )
}