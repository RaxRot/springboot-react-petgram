import { motion } from "framer-motion"
import { cn } from "@/lib/cn"

export default function PetCard({ pet }) {
    const colorMap = {
        DOG: "neon-dog",
        CAT: "neon-cat",
        BIRD: "neon-bird",
        FISH: "neon-fish",
        PIG: "neon-pig",
        OTHER: "neon-other",
    }

    const neon = colorMap[pet.type?.toUpperCase()] || "neon-other"

    return (
        <motion.div
            whileHover={{ scale: 1.04, rotate: 0.5 }}
            whileTap={{ scale: 0.97 }}
            transition={{ type: "spring", stiffness: 200, damping: 12 }}
            className={cn(
                "relative overflow-hidden rounded-2xl p-6 text-white shadow-xl backdrop-blur-xl transition-all duration-500",
                "border border-white/10 hover:border-white/20",
                "bg-white/5 dark:bg-gray-900/40",
                "hover:shadow-[0_0_25px_var(--color-" + neon + ")] group"
            )}
        >
            {/* 🌈 Animated glow border */}
            <div
                className={cn(
                    "absolute inset-0 rounded-2xl opacity-30 blur-xl transition-all duration-700 group-hover:opacity-70",
                    "bg-[radial-gradient(circle_at_top_left,var(--color-" +
                    neon +
                    ")_0%,transparent_70%)]"
                )}
            />

            {/* 🐾 Content */}
            <div className="relative z-10 flex items-center gap-4">
                {/* Avatar bubble */}
                <div
                    className={cn(
                        "w-16 h-16 rounded-full flex items-center justify-center text-3xl font-bold",
                        "bg-gradient-to-br from-[var(--color-" +
                        neon +
                        ")] to-[var(--color-" +
                        neon +
                        ")/70] shadow-lg animate-pulse-soft"
                    )}
                >
                    {pet.icon || "🐾"}
                </div>

                {/* Name & Type */}
                <div>
                    <h3
                        className={cn(
                            "text-xl font-extrabold tracking-wide",
                            "bg-gradient-to-r from-[var(--color-" +
                            neon +
                            ")] to-[var(--color-" +
                            neon +
                            ")/70] bg-clip-text text-transparent"
                        )}
                    >
                        {pet.name || "Unknown Pet"}
                    </h3>
                    <p className="text-sm text-gray-400 uppercase tracking-wider">
                        {pet.type || "Other"}
                    </p>
                </div>
            </div>

            {/* Description */}
            {pet.description && (
                <p className="relative z-10 mt-4 text-sm leading-relaxed text-gray-300 dark:text-gray-400">
                    {pet.description}
                </p>
            )}

            {/* Hover shimmer line */}
            <motion.div
                initial={{ opacity: 0, x: "-100%" }}
                whileHover={{ opacity: 1, x: "100%" }}
                transition={{ duration: 1.5, repeat: Infinity }}
                className={cn(
                    "absolute top-0 left-0 w-full h-[1px] bg-gradient-to-r from-transparent via-[var(--color-" +
                    neon +
                    ")] to-transparent"
                )}
            />
        </motion.div>
    )
}
