import { useEffect } from "react"
import { useParams, useNavigate, Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import Skeleton from "@/components/ui/Skeleton"

export default function StoryView() {
    const { id } = useParams()
    const nav = useNavigate()

    const { data, isLoading, error } = useQuery({
        queryKey: ["story", id],
        queryFn: async () => (await api.get(`/api/public/stories/${id}`)).data,
    })

    useEffect(() => {
        const timer = setTimeout(() => nav("/"), 5000)
        return () => clearTimeout(timer)
    }, [nav])

    if (isLoading)
        return (
            <div className="flex items-center justify-center h-screen">
                <Skeleton className="w-96 h-96 rounded-2xl" variant="image" />
            </div>
        )

    if (error)
        return (
            <div className="flex flex-col items-center justify-center h-screen text-center space-y-4">
                <h1 className="text-3xl font-bold text-red-500">Story not found 😿</h1>
                <Button onClick={() => nav("/")}>Back to Feed</Button>
            </div>
        )

    const s = data

    return (
        <div className="fixed inset-0 bg-black/90 dark:bg-[#0b0f17]/95 flex flex-col items-center justify-center p-4">
            <div className="relative w-full max-w-md">
                <img
                    src={s.imageUrl}
                    alt="story"
                    className="rounded-2xl shadow-[0_0_40px_rgba(56,189,248,0.15)] object-contain w-full h-[80vh]"
                />

                {/* 👤 Автор */}
                {s.authorUsername ? (
                    <Link
                        to={`/profile/${s.authorUsername}`}
                        className="absolute top-3 left-3 bg-black/60 text-white text-sm px-3 py-1 rounded-full hover:bg-black/80 transition"
                    >
                        @{s.authorUsername}
                    </Link>
                ) : s.authorId ? (
                    <div className="absolute top-3 left-3 bg-black/60 text-white text-sm px-3 py-1 rounded-full">
                        User #{s.authorId}
                    </div>
                ) : (
                    <div className="absolute top-3 left-3 bg-black/60 text-white text-sm px-3 py-1 rounded-full">
                        by Anonymous
                    </div>
                )}
            </div>

            <div className="mt-6 text-gray-300 text-sm text-center">
                Auto-close in 5 s ⏳
            </div>

            <Button
                onClick={() => nav("/")}
                className="mt-4 bg-gradient-to-r from-cyan-400 to-purple-600 text-white px-6 py-2 rounded-xl"
            >
                Close
            </Button>
        </div>
    )
}
