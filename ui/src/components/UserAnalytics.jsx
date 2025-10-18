import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Skeleton from "@/components/ui/Skeleton"
import { motion } from "framer-motion"
import {
    BarChart3,
    Heart,
    MessageSquare,
    Eye,
    Users,
    PawPrint,
    HandCoins,
} from "lucide-react"

export default function UserAnalytics() {
    const { data, isLoading, error } = useQuery({
        queryKey: ["userStats"],
        queryFn: async () => (await api.get("/api/user/stats")).data,
    })

    if (isLoading)
        return (
            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6 mt-8">
                {[...Array(8)].map((_, i) => (
                    <Skeleton key={i} className="h-28 w-full rounded-2xl" variant="card" />
                ))}
            </div>
        )

    if (error)
        return (
            <div className="text-center text-red-500 mt-6">
                ⚠️ Failed to load analytics
            </div>
        )

    const s = data || {}

    const stats = [
        { label: "Posts", value: s.totalPosts, icon: BarChart3, color: "from-cyan-400 to-purple-600" },
        { label: "Likes", value: s.totalLikes, icon: Heart, color: "from-pink-500 to-rose-500" },
        { label: "Comments", value: s.totalComments, icon: MessageSquare, color: "from-blue-400 to-cyan-400" },
        { label: "Views", value: s.totalViews, icon: Eye, color: "from-yellow-400 to-orange-500" },
        { label: "Pets", value: s.totalPets, icon: PawPrint, color: "from-green-400 to-emerald-500" },
        { label: "Followers", value: s.totalFollowers, icon: Users, color: "from-violet-400 to-purple-500" },
        { label: "Following", value: s.totalFollowing, icon: Users, color: "from-indigo-400 to-blue-500" },
        {
            label: "Donations",
            value: `$${(s.totalDonationsReceived / 100).toFixed(2)}`,
            icon: HandCoins,
            color: "from-amber-400 to-yellow-500",
        },
    ]

    return (
        <section className="mt-12">
            <h2 className="text-3xl font-extrabold mb-8 bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                Analytics ✨
            </h2>

            <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-8">
                {stats.map(({ label, value, icon: Icon, color }, i) => (
                    <motion.div
                        key={label}
                        initial={{ opacity: 0, y: 25 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.05, duration: 0.4 }}
                        whileHover={{ scale: 1.05, y: -5 }}
                        className={`
              relative rounded-2xl p-5 backdrop-blur-xl border border-white/10 overflow-hidden
              shadow-[0_0_15px_rgba(255,255,255,0.05)]
              hover:shadow-[0_0_25px_rgba(255,255,255,0.1)]
              transition-all duration-500 group
            `}
                    >
                        {/* Gradient Glow Border */}
                        <div
                            className={`absolute inset-0 opacity-0 group-hover:opacity-100 blur-xl bg-gradient-to-r ${color} transition-all duration-700`}
                        ></div>

                        {/* Inner glass panel */}
                        <div className="relative z-10 flex items-center gap-4">
                            <div
                                className={`p-3 rounded-xl bg-gradient-to-br ${color} text-white shadow-lg animate-pulse-soft`}
                            >
                                <Icon className="w-6 h-6" />
                            </div>

                            <div>
                                <div className="text-2xl font-bold text-white drop-shadow-[0_0_8px_rgba(255,255,255,0.2)]">
                                    {value ?? 0}
                                </div>
                                <div className="text-sm text-gray-400 tracking-wide">
                                    {label}
                                </div>
                            </div>
                        </div>

                        {/* Glow reflection */}
                        <motion.div
                            initial={{ opacity: 0 }}
                            animate={{ opacity: [0, 0.6, 0] }}
                            transition={{ duration: 3, repeat: Infinity, delay: i * 0.3 }}
                            className={`absolute -top-1 -left-1 w-[calc(100%+4px)] h-[calc(100%+4px)] rounded-2xl bg-gradient-to-r ${color} blur-md opacity-30`}
                        />
                    </motion.div>
                ))}
            </div>
        </section>
    )
}
