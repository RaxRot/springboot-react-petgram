import { cn } from "@/lib/cn"

export default function Skeleton({ className, variant = "default" }) {
    const variants = {
        default: "bg-gradient-to-r from-gray-800 via-gray-700 to-gray-800",
        card: "bg-gradient-to-r from-gray-800/80 via-gray-700/80 to-gray-800/80 backdrop-blur-sm border border-white/10",
        text: "bg-gradient-to-r from-gray-800 via-gray-700 to-gray-800 h-4",
        image: "bg-gradient-to-r from-gray-800 via-gray-700 to-gray-800 rounded-2xl border border-white/10",
        button: "bg-gradient-to-r from-gray-800 via-gray-700 to-gray-800 rounded-xl"
    }

    return (
        <div
            className={cn(
                "relative overflow-hidden",
                // Base animation
                "animate-pulse-slow",
                // Variant styles
                variants[variant],
                // Shimmer effect overlay
                "after:absolute after:inset-0 after:-translate-x-full after:bg-gradient-to-r after:from-transparent after:via-white/10 after:to-transparent",
                "after:animate-shimmer after:duration-2000",
                className
            )}
        />
    )
}