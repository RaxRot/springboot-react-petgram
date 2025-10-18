import { cn } from "@/lib/cn"

export default function Skeleton({ className, variant = "default" }) {
    const variants = {
        default:
            "h-4 w-full rounded-md bg-gradient-to-r from-gray-300/20 via-gray-400/10 to-gray-300/20 dark:from-gray-800/60 dark:via-gray-700/60 dark:to-gray-800/60",
        card:
            "h-24 w-full rounded-2xl border border-white/10 backdrop-blur-xl bg-gradient-to-br from-gray-900/40 via-gray-800/30 to-gray-900/40 shadow-[0_0_15px_rgba(255,255,255,0.05)]",
        text:
            "h-4 w-3/4 rounded-md bg-gradient-to-r from-gray-300/30 via-gray-400/10 to-gray-300/30 dark:from-gray-800/70 dark:via-gray-700/50 dark:to-gray-800/70",
        image:
            "h-40 w-full rounded-2xl bg-gradient-to-br from-gray-900/50 via-gray-800/40 to-gray-900/50 border border-white/10 backdrop-blur-lg shadow-[0_0_25px_rgba(255,255,255,0.05)]",
        button:
            "h-10 w-24 rounded-xl bg-gradient-to-r from-gray-900/60 via-gray-800/40 to-gray-900/60 border border-white/10 backdrop-blur-md",
    }

    return (
        <div
            className={cn(
                "relative overflow-hidden isolate",
                "before:absolute before:inset-0 before:-translate-x-full before:bg-gradient-to-r before:from-transparent before:via-cyan-400/10 before:to-transparent",
                "before:animate-[shimmer_2s_infinite]",
                "after:absolute after:inset-0 after:rounded-xl after:opacity-10 after:blur-2xl after:bg-gradient-to-br after:from-cyan-400/10 after:to-purple-500/10",
                variants[variant],
                className
            )}
        />
    )
}
