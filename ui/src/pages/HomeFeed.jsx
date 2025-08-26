import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { Link, useSearchParams } from "react-router-dom"
import Button from "@/components/ui/Button"

const ANIMALS = ["ALL","DOG","CAT","BIRD","FISH","PIG","OTHER"]
const TABS = ["ALL","FOLLOWING"]

export default function HomeFeed() {
    const [sp, setSp] = useSearchParams()
    const tab  = (sp.get("tab")  || "ALL").toUpperCase()
    const type = (sp.get("type") || "ALL").toUpperCase()

    const { data, isLoading, error } = useQuery({
        queryKey: ["feed", tab, type],
        queryFn: async () => {
            if (tab === "FOLLOWING") {
                // защищённый фид по подпискам
                const { data } = await api.get(
                    "/api/posts/feed/following?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc"
                )
                return data
            }
            // публичный фид
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

    if (isLoading) return <p>Loading feed…</p>
    if (error)     return <p className="text-red-500">{error.message}</p>

    const setTab = (t) => setSp(prev => {
        const p = new URLSearchParams(prev)
        p.set("tab", t)
        if (t === "FOLLOWING") p.delete("type") // фильтры только для ALL
        return p
    })

    const setType = (t) => setSp(prev => {
        const p = new URLSearchParams(prev)
        p.set("tab", "ALL")
        p.set("type", t)
        return p
    })

    return (
        <div className="grid gap-4">
            {/* Вкладки */}
            <div className="flex gap-2 mb-1">
                {TABS.map(t => (
                    <Button
                        key={t}
                        variant={t === tab ? "primary" : "outline"}
                        onClick={() => setTab(t)}
                        className="px-3 py-1"
                    >
                        {t}
                    </Button>
                ))}
            </div>

            {/* Фильтры животных видимы только во вкладке ALL */}
            {tab === "ALL" && (
                <div className="flex flex-wrap gap-2 mb-2">
                    {ANIMALS.map(a => (
                        <Button
                            key={a}
                            variant={a === type ? "primary" : "outline"}
                            onClick={() => setType(a)}
                            className="px-3 py-1"
                        >
                            {a}
                        </Button>
                    ))}
                </div>
            )}

            {/* Сетка постов */}
            {(!data?.content || data.content.length === 0) && (
                <div className="opacity-70">
                    {tab === "FOLLOWING"
                        ? "Your following feed is empty. Follow someone to see posts here."
                        : "No posts yet."}
                </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {data?.content?.map(p => (
                    <Link
                        key={p.id}
                        to={`/posts/${p.id}`}
                        className="rounded-2xl border p-4 hover:bg-[--color-muted] transition"
                    >
                        <div className="text-xs opacity-60">{p.animalType}</div>
                        <h3 className="text-lg font-semibold line-clamp-2">{p.title}</h3>

                        {p.imageUrl && (
                            <div className="mt-3 rounded-xl overflow-hidden bg-black/5">
                                <div className="relative aspect-[4/3]">
                                    <img
                                        src={p.imageUrl}
                                        alt={p.title}
                                        className="absolute inset-0 w-full h-full object-cover"
                                    />
                                </div>
                            </div>
                        )}
                    </Link>
                ))}
            </div>
        </div>
    )
}
