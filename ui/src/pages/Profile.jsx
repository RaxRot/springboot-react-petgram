import { useRef, useState, useEffect } from "react"
import { useParams, Link, useNavigate } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"

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
            toast.success("Avatar updated")
            qc.invalidateQueries({ queryKey: ["publicUser", username] })
        } catch {
            toast.error("Failed to update avatar")
        }
    }

    const follow = useMutation({
        mutationFn: async () => api.post(`/api/users/${profileQ.data.id}/follow`),
        onSuccess: () => {
            toast.success("Followed")
            qc.invalidateQueries({ queryKey: ["publicUser", username] })
            qc.invalidateQueries({ queryKey: ["followState", username] })
        },
    })

    const unfollow = useMutation({
        mutationFn: async () => api.delete(`/api/users/${profileQ.data.id}/follow`),
        onSuccess: () => {
            toast.info("Unfollowed")
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
            else toast.error("No session URL from Stripe")
        },
        onError: (e) => {
            const msg = e?.response?.data?.message || "Checkout failed"
            toast.error(msg)
        },
    })

    if (profileQ.isError) {
        return (
            <div className="space-y-3">
                <p className="text-red-500">User not found</p>
                {user?.username && user.username !== username && (
                    <Button onClick={() => nav(`/u/${user.username}`, { replace: true })}>
                        Go to @{user.username}
                    </Button>
                )}
            </div>
        )
    }

    if (profileQ.isLoading) return <p>Loading profile...</p>

    const prof = profileQ.data
    const following = !!followStateQ.data?.following

    return (
        <div className="space-y-6">
            <div className="flex items-center gap-4">
                <div className="relative">
                    <img
                        src={prof.profilePic || "https://placehold.co/96x96?text=🐾"}
                        className="h-24 w-24 rounded-full object-cover border"
                        alt=""
                    />
                    {isMe && (
                        <>
                            <input ref={fileRef} type="file" className="hidden" accept="image/*" onChange={onFile} />
                            <Button className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-3 py-1" onClick={onPickFile}>
                                Change
                            </Button>
                        </>
                    )}
                </div>

                <div className="space-y-1">
                    <h1 className="text-2xl font-bold">@{prof.userName}</h1>
                    <div className="text-sm opacity-70">
                        <span className="mr-4">Followers: {prof.followers}</span>
                        <span>Following: {prof.following}</span>
                    </div>

                    {!isMe && user && (
                        <div className="flex flex-wrap items-center gap-2">
                            {following ? (
                                <Button variant="outline" onClick={() => unfollow.mutate()}>Unfollow</Button>
                            ) : (
                                <Button onClick={() => follow.mutate()}>Follow</Button>
                            )}

                            {/* NEW: Message */}
                            <Button
                                variant="outline"
                                onClick={() => nav(`/messages/${encodeURIComponent(prof.userName)}`)}
                            >
                                Message
                            </Button>

                            <div className="flex items-center gap-2 ml-2">
                                <select
                                    value={donateAmount}
                                    onChange={(e) => setDonateAmount(Number(e.target.value))}
                                    className="rounded-xl border px-2 py-1"
                                >
                                    <option value={200}>$2</option>
                                    <option value={500}>$5</option>
                                    <option value={1000}>$10</option>
                                </select>
                                <Button
                                    onClick={() => {
                                        if (donateAmount < 100) { toast.error("Minimum is $1"); return }
                                        donate.mutate()
                                    }}
                                    disabled={donate.isPending || !profileQ.data?.id}
                                >
                                    {donate.isPending ? "Processing..." : "Donate"}
                                </Button>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            <section className="space-y-3">
                <h2 className="text-xl font-semibold">Posts</h2>
                {postsQ.isLoading && <p>Loading posts…</p>}
                {postsQ.data?.content?.length === 0 && <p>No posts yet.</p>}

                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {postsQ.data?.content?.map((p) => (
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
                                        <img src={p.imageUrl} alt={p.title} className="absolute inset-0 w-full h-full object-cover" />
                                    </div>
                                </div>
                            )}
                        </Link>
                    ))}
                </div>
            </section>
        </div>
    )
}
