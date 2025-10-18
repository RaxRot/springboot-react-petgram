import { useParams, Link } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import { useAuth } from "@/store/auth"
import { useState } from "react"
import Input from "@/components/ui/Input"
import FollowButton from "@/components/FollowButton"
import { confirmToast } from "@/components/ui/Confirm"
import ResponsiveImage from "@/components/ui/ResponsiveImage"
import Skeleton from "@/components/ui/Skeleton"
import Poll from "@/components/posts/Poll.jsx"

export default function PostDetails() {
    const { id } = useParams()
    const qc = useQueryClient()
    const { user, isAdmin } = useAuth()
    const [comment, setComment] = useState("")

    const postQ = useQuery({
        queryKey: ["post", id],
        queryFn: async () => (await api.get(`/api/public/posts/${id}`)).data,
    })

    const likesCountQ = useQuery({
        queryKey: ["likes", id],
        queryFn: async () => (await api.get(`/api/public/posts/${id}/likes`)).data.likesCount,
    })

    const commentsQ = useQuery({
        queryKey: ["comments", id],
        queryFn: async () =>
            (await api.get(`/api/public/posts/${id}/comments?pageNumber=0&pageSize=50&sortBy=createdAt&sortOrder=asc`)).data,
    })

    const authorUsername = postQ.data?.user?.userName
    const authorIdQ = useQuery({
        queryKey: ["author-id", authorUsername],
        enabled: !!authorUsername,
        queryFn: async () => {
            const { data } = await api.get(`/api/public/users/username/${authorUsername}/id`)
            return typeof data === "number" ? data : data?.id
        },
    })

    // === mutations ===
    const like = useMutation({
        mutationFn: async () => api.post(`/api/posts/${id}/likes`),
        onSuccess: () => qc.invalidateQueries({ queryKey: ["likes", id] }),
    })
    const unlike = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}/likes`),
        onSuccess: () => qc.invalidateQueries({ queryKey: ["likes", id] }),
    })
    const bookmark = useMutation({
        mutationFn: async () => api.post(`/api/posts/${id}/bookmarks`),
        onSuccess: () => toast.success("✅ Saved to bookmarks"),
    })
    const unbookmark = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}/bookmarks`),
        onSuccess: () => toast.info("🔔 Removed from bookmarks"),
    })
    const del = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}`),
        onSuccess: () => {
            toast.success("🗑️ Post deleted")
            window.history.back()
        },
        onError: () => toast.error("🚫 Failed to delete post"),
    })
    const addComment = useMutation({
        mutationFn: async (text) => api.post(`/api/posts/${id}/comments`, { text }),
        onSuccess: () => {
            setComment("")
            qc.invalidateQueries({ queryKey: ["comments", id] })
        },
    })
    const deleteComment = useMutation({
        mutationFn: async (cid) => api.delete(`/api/comments/${cid}`),
        onSuccess: () => {
            toast.success("🗑️ Comment removed")
            qc.invalidateQueries({ queryKey: ["comments", id] })
        },
    })
    const banUser = useMutation({
        mutationFn: async (userId) => api.patch(`/api/admin/users/ban/${userId}`),
        onSuccess: () => toast.success("🔨 User banned"),
        onError: () => toast.error("🚫 Failed to ban"),
    })

    if (postQ.isLoading)
        return (
            <div className="max-w-2xl mx-auto p-6 space-y-6">
                <Skeleton className="h-10 w-3/4" variant="text" />
                <Skeleton className="h-4 w-32" variant="text" />
                <Skeleton className="h-96 w-full rounded-2xl" variant="image" />
                <div className="flex gap-2 flex-wrap">
                    <Skeleton className="h-10 w-20 rounded-xl" variant="button" />
                    <Skeleton className="h-10 w-20 rounded-xl" variant="button" />
                    <Skeleton className="h-10 w-20 rounded-xl" variant="button" />
                </div>
            </div>
        )

    if (postQ.error)
        return (
            <div className="max-w-2xl mx-auto p-6">
                <div className="border border-destructive/30 bg-destructive/10
                    text-destructive-foreground rounded-2xl p-4">
                    Error: {postQ.error.message}
                </div>
            </div>
        )

    const p = postQ.data
    const canDelete = user && (isAdmin() || user.username === p.user?.userName)

    const onDeleteClick = async () => {
        const ok = await confirmToast({
            title: "Delete this post?",
            desc: "This action cannot be undone.",
            okText: "Delete",
        })
        if (ok) del.mutate()
    }

    const playAnimalSound = () => {
        if (!p?.animalType) return
        const sound = new Audio(`/sounds/${p.animalType.toLowerCase()}.mp3`)
        sound
            .play()
            .then(() => toast.success(`🎵 ${p.animalType} sound playing...`))
            .catch(() => toast.error("⚠️ Unable to play sound"))
    }

    return (
        <div className="max-w-2xl mx-auto p-6 space-y-6">

            {/* Header */}
            <div className="p-6 rounded-2xl border border-border
                bg-card text-foreground
                backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]">
                <h1 className="text-3xl font-bold">{p.title}</h1>

                {p.animalType && (
                    <div className="flex items-center gap-3 mt-2">
                        <div className="text-sm text-ring font-medium">
                            {p.animalType}
                        </div>
                        <Button
                            variant="outline"
                            onClick={playAnimalSound}
                            className="flex items-center gap-2 hover:shadow-[0_0_20px_hsl(var(--ring))] transition-all"
                        >
                            🔊 Play Sound
                        </Button>
                    </div>
                )}

                <div className="flex items-center gap-3 mt-4">
                    <Link to={`/u/${p.user?.userName}`} className="flex items-center gap-2 group">
                        <div className="w-10 h-10 bg-gradient-to-r from-cyan-400 to-purple-600 rounded-full flex items-center justify-center text-white font-bold">
                            {p.user?.userName?.[0]?.toUpperCase()}
                        </div>
                        <span className="group-hover:text-ring transition-colors">
                            @{p.user?.userName ?? "user"}
                        </span>
                    </Link>

                    {authorIdQ.data && user && user.username !== p.user?.userName && (
                        <FollowButton followeeId={authorIdQ.data} />
                    )}
                </div>

                {p.content && (
                    <p className="mt-4 text-muted-foreground leading-relaxed">{p.content}</p>
                )}
            </div>

            {/* Image */}
            {p.imageUrl && (
                <div className="rounded-2xl overflow-hidden border border-border
                    bg-card">
                    <ResponsiveImage src={p.imageUrl} alt={p.title} />
                </div>
            )}

            {/* Poll */}
            <Poll postId={id} postAuthorUsername={p.user?.userName} />

            {/* Actions */}
            <div className="p-4 rounded-2xl border border-border
                bg-card text-foreground
                backdrop-blur-xl">
                <div className="flex flex-wrap items-center gap-3">
                    <Button onClick={() => like.mutate()} variant="outline" className="flex items-center gap-2">
                        ❤️ Like
                    </Button>
                    <Button onClick={() => unlike.mutate()} variant="ghost" className="flex items-center gap-2">
                        💔 Unlike
                    </Button>

                    <span className="text-lg font-semibold text-ring">
                        ❤ {likesCountQ.data ?? 0}
                    </span>

                    <div className="flex-1"></div>

                    <Button onClick={() => bookmark.mutate()} variant="outline" className="flex items-center gap-2">
                        🔖 Save
                    </Button>
                    <Button onClick={() => unbookmark.mutate()} variant="ghost" className="flex items-center gap-2">
                        📑 Unsave
                    </Button>

                    {canDelete && (
                        <Button variant="danger" onClick={onDeleteClick} className="flex items-center gap-2">
                            🗑️ Delete
                        </Button>
                    )}
                </div>
            </div>

            {/* Comments */}
            <section className="p-6 rounded-2xl border border-border
                bg-card text-foreground
                backdrop-blur-xl space-y-4">
                <h2 className="text-2xl font-bold mb-4">Comments 💬</h2>

                {commentsQ.data?.content?.map((c) => (
                    <div key={c.id}
                         className="p-4 rounded-xl border border-border/50
                            bg-muted/20 text-foreground">
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1">
                                <div className="flex items-center gap-2 mb-2">
                                    <div className="w-8 h-8 bg-gradient-to-r from-cyan-400 to-purple-600 rounded-full flex items-center justify-center text-white text-xs font-bold">
                                        {c.author?.userName?.[0]?.toUpperCase()}
                                    </div>
                                    <span className="text-sm font-medium">@{c.author?.userName ?? "user"}</span>
                                </div>
                                <p className="text-muted-foreground">{c.text}</p>
                            </div>

                            <div className="flex items-center gap-2">
                                {user && (isAdmin() || user.id === c.author?.id) && (
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => deleteComment.mutate(c.id)}
                                        className="px-3 py-1 text-sm"
                                    >
                                        🗑️
                                    </Button>
                                )}
                                {isAdmin() && c.author?.id && (
                                    <Button
                                        size="sm"
                                        variant="outline"
                                        onClick={async () => {
                                            const ok = await confirmToast({
                                                title: `Ban @${c.author?.userName}?`,
                                                desc: "The user will lose posting/commenting ability.",
                                                okText: "Ban",
                                            })
                                            if (ok) banUser.mutate(c.author.id)
                                        }}
                                        className="px-3 py-1 text-sm"
                                    >
                                        🔨
                                    </Button>
                                )}
                            </div>
                        </div>
                    </div>
                ))}

                {user && (
                    <form
                        onSubmit={(e) => {
                            e.preventDefault()
                            if (comment.trim()) addComment.mutate(comment.trim())
                        }}
                        className="flex gap-3 mt-6"
                    >
                        <Input
                            placeholder="Write your comment…"
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            className="flex-1"
                        />
                        <Button type="submit" disabled={!comment.trim()} className="px-6">
                            💬 Send
                        </Button>
                    </form>
                )}
            </section>
        </div>
    )
}