import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Skeleton from "@/components/ui/Skeleton"
import { motion } from "framer-motion"

export default function AdminDashboard() {
    const statsQ = useQuery({
        queryKey: ["adminStats"],
        queryFn: async () => (await api.get("/api/admin/users/stats")).data,
    })

    if (statsQ.isLoading)
        return (
            <div className="p-8 grid gap-8 md:grid-cols-2 xl:grid-cols-4">
                {[...Array(4)].map((_, i) => (
                    <Skeleton key={i} className="h-32 rounded-2xl" variant="card" />
                ))}
            </div>
        )

    if (statsQ.error)
        return (
            <div className="p-8">
                <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                    Error: {statsQ.error.message}
                </div>
            </div>
        )

    const stats = statsQ.data || {
        users: 0,
        donations: 0,
        comments: 0,
        posts: 0,
    }

    const cards = [
        { label: "Users", value: stats.users, color: "from-cyan-400 to-blue-600", icon: "👥" },
        { label: "Donations", value: stats.donations, color: "from-amber-400 to-orange-500", icon: "💰" },
        { label: "Comments", value: stats.comments, color: "from-pink-500 to-purple-600", icon: "💬" },
        { label: "Posts", value: stats.posts, color: "from-emerald-400 to-green-600", icon: "🐾" },
    ]

    return (
        <div className="p-8 space-y-12 max-w-7xl mx-auto">
            {/* 🌈 Header */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6 }}
                className="relative flex flex-col sm:flex-row items-center justify-between gap-4"
            >
                <h1 className="text-4xl font-extrabold bg-gradient-to-r from-cyan-400 via-purple-500 to-pink-500 bg-clip-text text-transparent">
                    🧩 Admin Control Center
                </h1>
                <p className="text-sm text-gray-400 italic">
                    Updated at {new Date().toLocaleTimeString()}
                </p>

                {/* animated background blur bubble */}
                <div className="absolute -top-12 -right-12 w-48 h-48 bg-cyan-500/20 blur-3xl rounded-full animate-pulse-slow" />
            </motion.div>

            {/* 📊 Cards */}
            <div className="grid gap-8 sm:grid-cols-2 xl:grid-cols-4">
                {cards.map(({ label, value, color, icon }, i) => (
                    <motion.div
                        key={label}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.1, duration: 0.6 }}
                        whileHover={{ scale: 1.05, rotate: 0.5 }}
                        className={`
              relative overflow-hidden rounded-2xl p-6 text-white
              bg-gradient-to-br ${color} shadow-lg shadow-black/30
              transition-all duration-500 hover:shadow-xl hover:shadow-cyan-500/20
            `}
                    >
                        {/* floating glow */}
                        <div className="absolute inset-0 bg-white/10 opacity-10 hover:opacity-20 transition-opacity duration-500" />
                        <div className="relative z-10 flex flex-col items-center text-center">
                            <div className="text-5xl mb-2 animate-bounce">{icon}</div>
                            <div className="text-5xl font-bold tracking-tight">{value}</div>
                            <div className="text-sm uppercase tracking-wider opacity-80">{label}</div>
                        </div>
                    </motion.div>
                ))}
            </div>

            {/* 🧠 Overview */}
            <motion.div
                initial={{ opacity: 0, y: 25 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, delay: 0.4 }}
                className="relative bg-gradient-to-br from-gray-900/80 to-black/90 border border-white/10 rounded-2xl p-8 backdrop-blur-2xl shadow-inner shadow-cyan-500/10"
            >
                <h2 className="text-2xl font-semibold text-white mb-6">
                    📈 Quick Overview
                </h2>

                <ul className="grid sm:grid-cols-2 gap-3 text-gray-300">
                    <li>👥 <span className="text-cyan-400 font-semibold">{stats.users}</span> registered users</li>
                    <li>💰 <span className="text-yellow-400 font-semibold">{stats.donations}</span> total donations</li>
                    <li>💬 <span className="text-pink-400 font-semibold">{stats.comments}</span> comments posted</li>
                    <li>🐾 <span className="text-green-400 font-semibold">{stats.posts}</span> published posts</li>
                </ul>

                {/* glowing underline */}
                <div className="absolute bottom-0 left-0 right-0 h-1 bg-gradient-to-r from-cyan-400 via-purple-500 to-pink-500 rounded-b-2xl" />
            </motion.div>

            {/* 📢 Tip */}
            <p className="text-sm text-gray-500 italic text-center">
                ⚙️ Use the Admin menu to manage users, donations, and comments.
            </p>
        </div>
    )
}
