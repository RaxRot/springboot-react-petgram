import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link, useSearchParams } from "react-router-dom"
import Button from "@/components/ui/Button"
import Skeleton from "@/components/ui/Skeleton"
import ResponsiveImage from "@/components/ui/ResponsiveImage"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"

const ANIMALS = ["ALL", "DOG", "CAT", "BIRD", "FISH", "PIG", "OTHER"]
const ANIMAL_EMOJIS = {
    ALL: "🌍",
    DOG: "🐕",
    CAT: "🐈",
    BIRD: "🐦",
    FISH: "🐠",
    PIG: "🐷",
    OTHER: "🐾",
}

export default function HomeFeed() {
    const [sp, setSp] = useSearchParams()
    const { user } = useAuth()

    const TABS = user ? ["ALL", "FOLLOWING"] : ["ALL"]
    const tab = (sp.get("tab") || "ALL").toUpperCase()
    const type = (sp.get("type") || "ALL").toUpperCase()

    // === STORIES ===
    const storiesQ = useQuery({
        queryKey: ["stories"],
        queryFn: async () => {
            try {
                const { data } = await api.get("/api/public/stories")
                return data
            } catch {
                return { content: [] }
            }
        },
        staleTime: 60_000,
    })

    // === TRENDING ===
    const trendingQ = useQuery({
        queryKey: ["trending"],
        queryFn: async () =>
            (await api.get("/api/public/posts/trending?page=0&size=8")).data,
        staleTime: 60_000,
    })

    // === FEED ===
    const feedQ = useQuery({
        queryKey: ["feed", tab, type],
        queryFn: async () => {
            try {
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
            } catch {
                toast.error("Failed to load feed")
                return { content: [] }
            }
        },
    })

    const data = feedQ.data ?? { content: [] }

    // === FUNCTIONS ===
    const setTab = (t) =>
        setSp((prev) => {
            const p = new URLSearchParams(prev)
            if (t === "FOLLOWING" && !user) {
                toast.info("Sign in to view your Following feed")
                return p
            }
            p.set("tab", t)
            if (t === "FOLLOWING") p.delete("type")
            return p
        })

    const setType = (t) =>
        setSp((prev) => {
            const p = new URLSearchParams(prev)
            p.set("tab", "ALL")
            p.set("type", t)
            return p
        })

    // === UI ===
    return (
        <div className="max-w-7xl mx-auto p-6 space-y-10">

            {/* HEADER */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    PetSocial Feed
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] mt-2">
                    Discover amazing pet moments
                </p>
            </div>

            {/* STORIES */}
            <section
                className="rounded-2xl border border-[hsl(var(--border))]
        bg-[hsl(var(--card))] text-[hsl(var(--card-foreground))]
        backdrop-blur-xl shadow-[0_4px_25px_rgba(0,0,0,0.05)]
        dark:shadow-[0_0_25px_rgba(56,189,248,0.15)]
        transition-all duration-300 p-4"
            >
                <div className="flex items-center justify-between mb-3">
                    <h3 className="text-lg font-semibold">Stories · 24h</h3>
                    <Link
                        to={user ? "/stories/create" : "/signin"}
                        className="text-sm font-medium text-[hsl(var(--ring))] hover:underline"
                    >
                        + Add story
                    </Link>
                </div>

                <div
                    id="storiesScroll"
                    className="flex gap-4 overflow-x-auto py-2 px-1 scrollbar-thin scrollbar-thumb-[hsl(var(--ring))]/40 scrollbar-track-transparent snap-x snap-mandatory"
                >
                    {storiesQ.isLoading &&
                        [...Array(8)].map((_, i) => (
                            <Skeleton key={i} className="h-24 w-24 rounded-2xl" variant="image" />
                        ))}

                    {(storiesQ.data?.content?.length ?? 0) === 0 && !storiesQ.isLoading && (
                        <div className="text-sm text-[hsl(var(--muted-foreground))] px-2">
                            No stories yet
                        </div>
                    )}

                    {(storiesQ.data?.content ?? []).map((st) => (
                        <Link
                            key={st.id}
                            to={`/stories/${st.id}`}
                            className="group shrink-0 snap-start w-24 h-24 rounded-2xl overflow-hidden
              border border-[hsl(var(--border))]
              bg-[hsl(var(--card))] hover:scale-105 hover:shadow-[0_0_20px_hsl(var(--ring))]
              transition-all duration-300"
                            title={`@${st.authorUsername ?? "user"}`}
                        >
                            <img
                                src={st.imageUrl}
                                alt=""
                                className="w-full h-full object-cover group-hover:opacity-90 transition"
                            />
                        </Link>
                    ))}
                </div>
            </section>

            {/* TABS */}
            <div className="flex gap-2 justify-center">
                {TABS.map((t) => (
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

            {/* FILTERS */}
            {tab === "ALL" && (
                <div className="flex flex-wrap gap-2 justify-center">
                    {ANIMALS.map((a) => (
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

            {/* TRENDING */}
            <section className="space-y-3">
                <div className="flex items-center gap-2">
                    <span className="text-2xl">🔥</span>
                    <h2 className="text-xl font-bold text-[hsl(var(--foreground))]">
                        Trending today
                    </h2>
                </div>

                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    {trendingQ.isLoading &&
                        [...Array(4)].map((_, i) => (
                            <Skeleton key={i} className="h-56 w-full rounded-2xl" variant="image" />
                        ))}

                    {trendingQ.data?.content?.map((p) => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="relative block rounded-2xl overflow-hidden border border-[hsl(var(--border))]
              bg-[hsl(var(--card))] hover:shadow-[0_0_25px_hsl(var(--ring))]
              transition-all duration-300"
                        >
                            {p.imageUrl && <ResponsiveImage src={p.imageUrl} alt={p.title} />}
                            <div className="absolute inset-x-0 bottom-0 p-3 bg-gradient-to-t from-black/60 to-transparent">
                                <div className="text-white font-semibold line-clamp-1">{p.title}</div>
                                <div className="text-white/80 text-xs mt-1 flex items-center gap-2">
                                    <span>👀 {p.viewsCount ?? 0}</span>
                                    <span>·</span>
                                    <span>@{p.user?.userName}</span>
                                </div>
                            </div>
                        </Link>
                    ))}
                </div>
            </section>

            {/* POSTS */}
            {(!data?.content || data.content.length === 0) ? (
                <div className="text-center py-16">
                    <div className="text-6xl mb-4">{tab === "FOLLOWING" ? "👥" : "📝"}</div>
                    <p className="text-[hsl(var(--muted-foreground))] text-lg">
                        {tab === "FOLLOWING"
                            ? "Your following feed is empty"
                            : "No posts yet"}
                    </p>
                    <p className="text-[hsl(var(--muted-foreground))] text-sm">
                        {tab === "FOLLOWING"
                            ? "Follow someone to see their posts here"
                            : "Be the first to share a pet moment!"}
                    </p>
                </div>
            ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {data.content.map((p) => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="group relative block bg-[hsl(var(--card))] backdrop-blur-xl rounded-2xl p-4
              border border-[hsl(var(--border))]
              hover:border-[hsl(var(--ring))] hover:shadow-[0_0_25px_hsl(var(--ring))]
              transition-all duration-300"
                        >
                            <div className="flex items-center justify-between mb-2">
                                <div className="text-xs font-semibold text-[hsl(var(--ring))] flex items-center gap-1">
                                    <span>{ANIMAL_EMOJIS[p.animalType] || ANIMAL_EMOJIS.OTHER}</span>
                                    {p.animalType}
                                </div>

                                {typeof p.viewsCount === "number" && (
                                    <span className="text-[11px] px-2 py-1 rounded-full bg-[hsl(var(--muted))] border border-[hsl(var(--border))] text-[hsl(var(--foreground))]">
                    👀 {p.viewsCount}
                  </span>
                                )}
                            </div>

                            <h3 className="text-lg font-semibold text-[hsl(var(--foreground))] line-clamp-2 group-hover:text-[hsl(var(--ring))] transition-colors">
                                {p.title}
                            </h3>

                            <div className="text-sm text-[hsl(var(--muted-foreground))] mt-2">
                                by @{p.user?.userName ?? "unknown"}
                            </div>

                            {p.imageUrl && (
                                <div className="mt-3 rounded-xl overflow-hidden">
                                    <ResponsiveImage
                                        src={p.imageUrl}
                                        alt={p.title}
                                        className="group-hover:scale-105 transition-transform duration-300"
                                    />
                                </div>
                            )}
                        </Link>
                    ))}
                </div>
            )}
        </div>
    )
}
