import { cn } from "@/lib/cn"

export default function Button({ as: Comp = "button", className, variant = "primary", ...props }) {
    const styles = {
        primary: "bg-gradient-to-r from-cyan-400 to-purple-600 text-white shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 hover:from-cyan-500 hover:to-purple-700 border-0",
        ghost: "bg-transparent text-gray-700 hover:bg-white/10 backdrop-blur-sm border border-white/20 hover:border-white/30 hover:text-white",
        danger: "bg-gradient-to-r from-red-500 to-pink-600 text-white shadow-lg shadow-red-500/30 hover:shadow-red-500/50 hover:from-red-600 hover:to-pink-700 border-0",
        outline: "bg-transparent text-gray-700 border border-cyan-400/50 hover:border-cyan-400 hover:bg-cyan-400/10 backdrop-blur-sm hover:text-cyan-300",
        neon: "bg-transparent text-cyan-300 border border-cyan-400/30 shadow-lg shadow-cyan-500/20 hover:shadow-cyan-500/40 hover:bg-cyan-400/10 hover:text-white backdrop-blur-sm",
        glass: "bg-white/5 text-white border border-white/10 backdrop-blur-xl hover:bg-white/10 hover:border-white/20 shadow-lg shadow-cyan-500/10"
    }

    return (
        <Comp
            className={cn(
                // Base styles
                "relative inline-flex items-center justify-center rounded-xl px-5 py-2.5 text-sm font-semibold transition-all duration-300 ease-out",
                // Interactive effects
                "hover:scale-105 active:scale-95",
                "disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100",
                // Variant styles
                styles[variant],
                // Glow effect for primary and danger
                variant === 'primary' && "animate-pulse-slow",
                variant === 'danger' && "animate-pulse-slow",
                className
            )}
            {...props}
        />
    )
}