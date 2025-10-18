import { cn } from "@/lib/cn"

export default function Button({
                                   as: Comp = "button",
                                   className = "",
                                   variant = "primary",
                                   size = "md",
                                   glow = true,
                                   children,
                                   ...props
                               }) {
    const base =
        "relative inline-flex items-center justify-center font-semibold rounded-xl transition-all duration-300 ease-out focus:outline-none focus:ring-2 focus:ring-offset-2 select-none"

    const sizes = {
        sm: "px-3 py-1.5 text-sm",
        md: "px-5 py-2.5 text-base",
        lg: "px-6 py-3 text-lg",
    }

    const variants = {
        primary:
            "text-white bg-gradient-to-r from-cyan-400 to-purple-600 hover:from-cyan-500 hover:to-purple-700 shadow-[0_0_12px_rgba(56,189,248,0.3)] hover:shadow-[0_0_20px_rgba(147,51,234,0.5)] border-0",
        outline:
            "bg-transparent text-gray-800 dark:text-gray-200 border border-cyan-400/50 hover:border-cyan-400 hover:text-cyan-300 hover:bg-cyan-400/10 backdrop-blur-sm shadow-[inset_0_0_10px_rgba(56,189,248,0.15)]",
        ghost:
            "bg-transparent text-gray-800 dark:text-gray-300 hover:bg-white/10 dark:hover:bg-white/5 hover:text-cyan-400",
        danger:
            "text-white bg-gradient-to-r from-red-500 to-pink-600 hover:from-red-600 hover:to-pink-700 shadow-[0_0_15px_rgba(239,68,68,0.4)] hover:shadow-[0_0_25px_rgba(236,72,153,0.6)] border-0",
        glass:
            "text-white bg-white/5 border border-white/10 backdrop-blur-xl hover:bg-white/10 hover:border-white/20 shadow-[0_0_15px_rgba(56,189,248,0.2)]",
        neon:
            "text-cyan-300 border border-cyan-400/30 shadow-[0_0_10px_rgba(56,189,248,0.2)] hover:shadow-[0_0_25px_rgba(56,189,248,0.5)] hover:text-white hover:bg-cyan-400/10 backdrop-blur-sm",
    }

    return (
        <Comp
            className={cn(
                base,
                sizes[size],
                variants[variant],
                glow &&
                (variant === "primary" || variant === "danger") &&
                "after:content-[''] after:absolute after:inset-0 after:rounded-xl after:blur-lg after:opacity-40 after:bg-gradient-to-r after:from-cyan-400 after:to-purple-600 dark:after:from-purple-500 dark:after:to-cyan-400",
                "hover:scale-[1.05] active:scale-[0.97]",
                "disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100",
                className
            )}
            {...props}
        >
            <span className="relative z-10 flex items-center gap-2">{children}</span>
        </Comp>
    )
}
