import { useRef, useState, useEffect } from "react"
import { useParams, Link, useNavigate } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"
import Skeleton from "@/components/ui/Skeleton"
import ResponsiveImage from "@/components/ui/ResponsiveImage"

export default function Profile() {
    const { username } = useParams()
    const nav = useNavigate()
    const { user } = useAuth()
    const qc = useQueryClient()
    const fileRef = useRef(null)
    const isMe = user?.username === username
    const [donateAmount, setDonateAmount] = useState(500) // cents

    useEffect(() => {
        if (user?.username && username && user.username !== username && isMe) {
            nav(`/u/${user.username}`, { replace: true })
        }
    }, [user?.username, username, isMe, nav])

    const profileQ = useQuery({
        queryKey: ["publicUser", username],
        queryFn: async () => (await api.get(`/api/public/users/${encodeURIComponent(username)}`)).data,
        retry: 0,
    })

    const postsQ = useQuery({
        queryKey: ["postsByUser", username],
        queryFn: async () =>
            (await api.get(
                `/api/public/users/${encodeURIComponent(username)}/posts?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc`
            )).data,
    })

    const followStateQ = useQuery({
        queryKey: ["followState", username],
        enabled: !!user && !!profileQ.data?.id && !isMe,
        queryFn: async () => {
            try {
                const { data } = await api.get(`/api/users/${profileQ.data.id}/follow/state`)
                return data
            } catch {
                return { following: false }
            }
        },
    })

    const onPickFile = () => fileRef.current?.click()
    const onFile = async (e) => {
        const f = e.target.files?.[0]
        if (!f) return
        if (!f.type.startsWith("image/")) { toast.error("Please select an image file"); return }
        try {
            const fd = new FormData()
            fd.append("file", f)
            await api.patch("/api/user/uploadimg", fd, { headers: { "Content-Type": "multipart/form-data" } })
            toast.success("🎉 Avatar updated")
            qc.invalidateQueries({ queryKey: ["publicUser", username] })
        } catch {
            toast.error("🚫 Failed to update avatar")
        }
    }

    const follow = useMutation({
        mutationFn: async () => api.post(`/api/users/${profileQ.data.id}/follow`),
        onSuccess: () => {
            toast.success("✅ Followed")
            qc.invalidateQueries({ queryKey: ["publicUser", username] })
            qc.invalidateQueries({ queryKey: ["followState", username] })
        },
    })

    const unfollow = useMutation({
        mutationFn: async () => api.delete(`/api/users/${profileQ.data.id}/follow`),
        onSuccess: () => {
            toast.info("🔔 Unfollowed")
            qc.invalidateQueries({ queryKey: ["publicUser", username] })
            qc.invalidateQueries({ queryKey: ["followState", username] })
        },
    })

    const donate = useMutation({
        mutationFn: async () => {
            const payload = {
                authorId: profileQ.data.id,
                amount: donateAmount,
                currency: "usd",
                successUrl: `${window.location.origin}/payment-success`,
                cancelUrl: `${window.location.origin}/payment-cancel`,
            }
            const { data } = await api.post("/api/stripe/checkout", payload)
            return data
        },
        onSuccess: (data) => {
            if (data?.sessionUrl) window.location.href = data.sessionUrl
            else toast.error("🚫 No session URL from Stripe")
        },
        onError: (e) => {
            const msg = e?.response?.data?.message || "Checkout failed"
            toast.error(msg)
        },
    })

    if (profileQ.isError) {
        return (
            <div className="max-w-2xl mx-auto p-6 text-center">
                <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-8">
                    <div className="text-6xl mb-4">😢</div>
                    <h1 className="text-2xl font-bold text-red-600 dark:text-red-400 mb-2">User not found</h1>
                    <p className="text-gray-600 dark:text-gray-400 mb-4">The user @{username} doesn't exist</p>
                    {user?.username && user.username !== username && (
                        <Button
                            onClick={() => nav(`/u/${user.username}`, { replace: true })}
                            className="bg-gradient-to-r from-cyan-400 to-purple-600"
                        >
                            Go to @{user.username}
                        </Button>
                    )}
                </div>
            </div>
        )
    }

    if (profileQ.isLoading) return (
        <div className="max-w-2xl mx-auto p-6 space-y-6">
            <div className="flex items-center gap-6">
                <Skeleton className="w-24 h-24 rounded-full" variant="image"/>
                <div className="space-y-3">
                    <Skeleton className="h-8 w-48" variant="text"/>
                    <Skeleton className="h-4 w-32" variant="text"/>
                    <div className="flex gap-2">
                        <Skeleton className="h-10 w-24 rounded-xl" variant="button"/>
                        <Skeleton className="h-10 w-24 rounded-xl" variant="button"/>
                    </div>
                </div>
            </div>
        </div>
    )

    const prof = profileQ.data
    const following = !!followStateQ.data?.following

    return (
        <div className="max-w-4xl mx-auto p-6 space-y-8">
            {/* Profile Header */}
            <div className="bg-white dark:bg-white/5 backdrop-blur-xl rounded-3xl p-8 border border-gray-200 dark:border-white/10 shadow-2xl shadow-cyan-500/10">
                <div className="flex items-center gap-6">
                    {/* Avatar */}
                    <div className="relative group">
                        <div className="w-32 h-32 rounded-full bg-gradient-to-r from-cyan-400 to-purple-600 p-1">
                            <img
                                src={prof.profilePic || "https://placehold.co/128x128?text=🐾"}
                                className="w-full h-full rounded-full object-cover border-4 border-white dark:border-gray-900"
                                alt=""
                            />
                        </div>
                        {isMe && (
                            <>
                                <input ref={fileRef} type="file" className="hidden" accept="image/*" onChange={onFile} />
                                <Button
                                    className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-4 py-2 bg-white dark:bg-gray-800 text-gray-900 dark:text-white border-0 shadow-lg"
                                    onClick={onPickFile}
                                >
                                    📷 Change
                                </Button>
                            </>
                        )}
                    </div>

                    {/* User Info */}
                    <div className="flex-1 space-y-4">
                        <div>
                            <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                                @{prof.userName}
                            </h1>
                            <div className="flex gap-6 mt-2 text-sm text-gray-600 dark:text-gray-400">
                                <span className="font-semibold">{prof.followers} followers</span>
                                <span className="font-semibold">{prof.following} following</span>
                            </div>
                        </div>

                        {/* Actions */}
                        {!isMe && user && (
                            <div className="flex flex-wrap items-center gap-3">
                                {following ? (
                                    <Button
                                        variant="outline"
                                        onClick={() => unfollow.mutate()}
                                        className="flex items-center gap-2"
                                    >
                                        👥 Unfollow
                                    </Button>
                                ) : (
                                    <Button
                                        onClick={() => follow.mutate()}
                                        className="flex items-center gap-2"
                                    >
                                        ✨ Follow
                                    </Button>
                                )}

                                <Button
                                    variant="outline"
                                    onClick={() => nav(`/messages/${encodeURIComponent(prof.userName)}`)}
                                    className="flex items-center gap-2"
                                >
                                    💬 Message
                                </Button>

                                {/* Donation */}
                                <div className="flex items-center gap-2 ml-4 bg-white dark:bg-white/10 rounded-xl p-2 border border-gray-200 dark:border-white/20">
                                    <select
                                        value={donateAmount}
                                        onChange={(e) => setDonateAmount(Number(e.target.value))}
                                        className="bg-transparent text-gray-900 dark:text-white border-0 focus:ring-2 focus:ring-cyan-400 rounded-lg px-2 py-1"
                                    >
                                        <option value={200}>$2</option>
                                        <option value={500}>$5</option>
                                        <option value={1000}>$10</option>
                                        <option value={2000}>$20</option>
                                    </select>
                                    <Button
                                        onClick={() => {
                                            if (donateAmount < 100) { toast.error("Minimum is $1"); return }
                                            donate.mutate()
                                        }}
                                        disabled={donate.isPending || !profileQ.data?.id}
                                        className="px-4 py-2 bg-gradient-to-r from-green-500 to-cyan-600"
                                    >
                                        {donate.isPending ? "⏳ Processing..." : "💰 Donate"}
                                    </Button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Posts Section */}
            <section className="space-y-6">
                <h2 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    Posts 📝
                </h2>

                {postsQ.isLoading && (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {[...Array(6)].map((_, i) => (
                            <div key={i} className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10">
                                <Skeleton className="h-4 w-20 mb-3" variant="text"/>
                                <Skeleton className="h-6 w-full mb-4" variant="text"/>
                                <Skeleton className="h-48 w-full rounded-xl" variant="image"/>
                            </div>
                        ))}
                    </div>
                )}

                {postsQ.data?.content?.length === 0 && (
                    <div className="text-center py-12 bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl border border-gray-200 dark:border-white/10">
                        <div className="text-6xl mb-4">📝</div>
                        <p className="text-gray-600 dark:text-gray-400 text-lg">No posts yet</p>
                        <p className="text-gray-500 dark:text-gray-500 text-sm">This user hasn't shared any pet moments</p>
                    </div>
                )}

                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {postsQ.data?.content?.map((p) => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="block bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10
                                     hover:bg-gray-50 dark:hover:bg-white/10 hover:border-cyan-400/30 hover:shadow-lg hover:shadow-cyan-500/10
                                     transition-all duration-300 group"
                        >
                            <div className="text-xs font-semibold text-cyan-600 dark:text-cyan-400 mb-2">
                                {p.animalType}
                            </div>
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white line-clamp-2 group-hover:text-cyan-600 dark:group-hover:text-cyan-400 transition-colors">
                                {p.title}
                            </h3>
                            {p.imageUrl && (
                                <div className="mt-3 rounded-xl overflow-hidden">
                                    <ResponsiveImage src={p.imageUrl} alt={p.title} />
                                </div>
                            )}
                        </Link>
                    ))}
                </div>
            </section>
        </div>
    )
}