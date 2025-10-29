import { useRef, useState, useEffect } from "react"
import { useParams, Link, useNavigate } from "react-router-dom"
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { motion, useReducedMotion } from "framer-motion"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"
import Skeleton from "@/components/ui/Skeleton"
import ResponsiveImage from "@/components/ui/ResponsiveImage"
import UserAnalytics from "@/components/UserAnalytics"

export default function Profile() {
    const { username } = useParams()
    const nav = useNavigate()
    const { user } = useAuth()
    const qc = useQueryClient()
    const fileRef = useRef(null)
    const isMe = user?.username === username
    const prefersReducedMotion = useReducedMotion()
    const [donateAmount, setDonateAmount] = useState(500)

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

    const petsQ = useQuery({
        queryKey: ["petsByUser", username],
        queryFn: async () =>
            (await api.get(`/api/public/users/${encodeURIComponent(username)}/pets?page=0&size=3`)).data,
    })

    const onPickFile = () => fileRef.current?.click()
    const onFile = async (e) => {
        const f = e.target.files?.[0]
        if (!f) return
        if (!f.type.startsWith("image/")) {
            toast.error("Please select an image file")
            return
        }
        try {
            const fd = new FormData()
            fd.append("file", f)
            await api.patch("/api/user/uploadimg", fd, {
                headers: { "Content-Type": "multipart/form-data" },
            })
            toast.success("🎉 Avatar updated")
            qc.invalidateQueries({ queryKey: ["publicUser", username] })
        } catch {
            toast.error("🚫 Failed to update avatar")
        }
    }

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

    if (profileQ.isLoading) return <Skeleton className="h-40 w-full rounded-2xl" />

    const prof = profileQ.data

    // animation variants
    const gridVariants = {
        hidden: { opacity: 0 },
        visible: {
            opacity: 1,
            transition: { when: "beforeChildren", staggerChildren: 0.1 },
        },
    }
    const cardVariants = {
        hidden: { opacity: 0, y: 20, scale: 0.95 },
        visible: {
            opacity: 1,
            y: 0,
            scale: 1,
            transition: { duration: 0.4, ease: "easeOut" },
        },
    }

    return (
        <div className="max-w-4xl mx-auto p-6 space-y-10">

            {/* Profile Header */}
            <div className="rounded-3xl p-8 border border-border
                bg-card text-foreground
                backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
                transition-all duration-300">

                <div className="flex flex-col sm:flex-row items-center gap-6">

                    {/* Avatar */}
                    <div className="relative group">
                        <div className="w-32 h-32 rounded-full bg-gradient-to-r from-cyan-400 to-purple-600 p-1">
                            <img
                                src={prof.profilePic || 'https://placehold.co/128x128?text=🐾'}
                                className="w-full h-full rounded-full object-cover border-4 border-card"
                                alt=""
                            />
                        </div>

                        {isMe && (
                            <>
                                <input ref={fileRef} type="file" className="hidden" accept="image/*" onChange={onFile} />
                                <Button
                                    className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-4 py-2
                                        bg-card text-foreground
                                        border border-border
                                        hover:shadow-[0_0_20px_hsl(var(--ring))]"
                                    onClick={onPickFile}>
                                    📷 Change
                                </Button>
                            </>
                        )}
                    </div>

                    {/* User Info */}
                    <div className="flex-1 space-y-4 text-center sm:text-left">
                        <div>
                            <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                                @{prof.userName}
                            </h1>
                            <div className="flex justify-center sm:justify-start gap-6 mt-2 text-sm text-muted-foreground">
                                <span className="font-semibold">{prof.followers} followers</span>
                                <span className="font-semibold">{prof.following} following</span>
                            </div>
                        </div>

                        {!isMe && user && (
                            <div className="flex flex-wrap items-center gap-3 justify-center sm:justify-start">
                                <Button
                                    variant="outline"
                                    onClick={() => nav(`/messages/${encodeURIComponent(prof.userName)}`)}
                                    className="flex items-center gap-2">
                                    💬 Message
                                </Button>

                                <div className="flex items-center gap-2 rounded-xl p-2
                                    border border-border
                                    bg-card
                                    text-foreground">
                                    <select
                                        value={donateAmount}
                                        onChange={(e) => setDonateAmount(Number(e.target.value))}
                                        className="bg-background text-foreground border border-border rounded-lg px-2 py-1 focus:ring-2 focus:ring-primary">
                                        <option value={200} className="bg-background text-foreground">$2</option>
                                        <option value={500} className="bg-background text-foreground">$5</option>
                                        <option value={1000} className="bg-background text-foreground">$10</option>
                                        <option value={2000} className="bg-background text-foreground">$20</option>
                                    </select>
                                    <Button
                                        onClick={() => {
                                            if (donateAmount < 100) {
                                                toast.error("Minimum is $1")
                                                return
                                            }
                                            donate.mutate()
                                        }}
                                        disabled={donate.isPending || !profileQ.data?.id}
                                        className="px-4 py-2 bg-gradient-to-r from-green-500 to-cyan-600">
                                        {donate.isPending ? "⏳ Processing..." : "💰 Donate"}
                                    </Button>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {isMe && (
                <div className="space-y-4">
                    <UserAnalytics />

                    <motion.div
                        className="text-right"
                        initial={{ opacity: 0, y: 15 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, ease: "easeOut" }}
                    >
                        <motion.button
                            onClick={async () => {
                                try {
                                    const res = await api.get("/api/user/stats/export", {
                                        responseType: "blob",
                                    })

                                    const url = window.URL.createObjectURL(new Blob([res.data]))
                                    const link = document.createElement("a")
                                    link.href = url
                                    link.setAttribute("download", "user_stats.pdf")
                                    document.body.appendChild(link)
                                    link.click()
                                    link.remove()
                                    window.URL.revokeObjectURL(url)

                                    toast.success("📄 Report downloaded successfully!")
                                } catch (err) {
                                    toast.error("🚫 Failed to download report")
                                    console.error(err)
                                }
                            }}
                            whileHover={{ scale: 1.06, boxShadow: "0px 0px 18px rgba(56,189,248,0.5)" }}
                            whileTap={{ scale: 0.95 }}
                            transition={{ type: "spring", stiffness: 250, damping: 15 }}
                            className="px-6 py-3 rounded-xl font-semibold text-white
                   bg-gradient-to-r from-cyan-500 to-purple-600
                   shadow-[0_0_20px_rgba(56,189,248,0.2)]
                   hover:shadow-[0_0_30px_rgba(56,189,248,0.4)]
                   focus:outline-none"
                        >
                            📄 Download my PDF report
                        </motion.button>
                    </motion.div>
                </div>
            )}

            {/* Pets Section */}
            <section className="space-y-6">
                <h2 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    Pets 🐾
                </h2>

                {petsQ.isLoading && (
                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {[...Array(3)].map((_, i) => (
                            <Skeleton key={i} className="h-48 w-full rounded-xl" variant="image" />
                        ))}
                    </div>
                )}

                {petsQ.data?.content?.length === 0 && (
                    <p className="text-muted-foreground italic">No pets yet 🐕</p>
                )}

                <motion.div
                    className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3"
                    variants={gridVariants}
                    initial="hidden"
                    animate="visible"
                >
                    {petsQ.data?.content?.map((pet) => (
                        <motion.div
                            key={pet.id}
                            variants={cardVariants}
                            whileHover={
                                prefersReducedMotion ? {} : { scale: 1.05, rotate: 0.8, y: -3 }
                            }
                            whileTap={prefersReducedMotion ? {} : { scale: 0.97 }}
                            className="relative bg-card rounded-2xl border border-border overflow-hidden
                                backdrop-blur-xl p-4 group transition-all duration-300
                                before:absolute before:inset-0 before:rounded-2xl before:p-[1px]
                                before:bg-gradient-to-br before:from-cyan-400/30 before:to-purple-600/30
                                before:opacity-0 group-hover:before:opacity-100 before:pointer-events-none"
                        >
                            <Link to={`/pets/${pet.id}`} className="block">
                                {pet.photoUrl && (
                                    <motion.div
                                        className="rounded-xl overflow-hidden mb-3"
                                        whileHover={{ scale: 1.06 }}
                                        transition={{ duration: 0.25 }}
                                    >
                                        <img
                                            src={pet.photoUrl}
                                            alt={pet.name}
                                            className="w-full h-48 object-cover"
                                        />
                                    </motion.div>
                                )}

                                <motion.h3
                                    className="text-xl font-bold text-foreground mt-1"
                                    whileHover={{ color: "#22d3ee" }}
                                    transition={{ duration: 0.2 }}
                                >
                                    {pet.name}
                                </motion.h3>
                                <p className="text-sm text-muted-foreground capitalize">
                                    {pet.type.toLowerCase()} • {pet.age || "?"} years
                                </p>
                                {pet.breed && (
                                    <p className="text-sm text-muted-foreground italic">{pet.breed}</p>
                                )}
                            </Link>

                            {/* glowing paw animation */}
                            <motion.div
                                initial={{ opacity: 0, scale: 0.4 }}
                                whileHover={{ opacity: 0.25, scale: 1 }}
                                className="absolute inset-0 flex items-center justify-center pointer-events-none text-[8rem] select-none"
                            >
                                🐾
                            </motion.div>
                        </motion.div>
                    ))}
                </motion.div>

                {isMe && (
                    <div className="text-right">
                        <Button
                            onClick={() => nav("/mypets")}
                            className="mt-4 bg-gradient-to-r from-cyan-400 to-purple-600">
                            Manage my pets →
                        </Button>
                    </div>
                )}
            </section>

            {/* Posts Section */}
            <section className="space-y-6">
                <h2 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    Posts 📝
                </h2>

                {postsQ.data?.content?.length === 0 && (
                    <div className="text-center py-12 rounded-2xl border border-border
                        bg-card text-foreground">
                        <div className="text-6xl mb-4">📝</div>
                        <p className="text-muted-foreground text-lg">No posts yet</p>
                    </div>
                )}

                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {postsQ.data?.content?.map((p) => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="group block rounded-2xl p-4 border border-border
                                bg-card text-foreground
                                hover:shadow-[0_0_25px_hsl(var(--ring))] transition-all duration-300">
                            {p.imageUrl && (
                                <div className="mt-3 rounded-xl overflow-hidden">
                                    <ResponsiveImage src={p.imageUrl} alt={p.title} />
                                </div>
                            )}
                            <h3 className="text-lg font-semibold mt-2 group-hover:text-ring transition-colors">
                                {p.title}
                            </h3>
                        </Link>
                    ))}
                </div>
            </section>
        </div>
    )
}
