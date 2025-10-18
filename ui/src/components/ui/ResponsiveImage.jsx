import { useState } from "react"
import { cn } from "@/lib/cn"
import { motion } from "framer-motion"

export default function ResponsiveImage({
                                            src,
                                            alt = "",
                                            className = "",
                                            withHoverEffect = true,
                                        }) {
    const [isLoaded, setIsLoaded] = useState(false)

    return (
        <motion.div
            initial={{ opacity: 0, scale: 0.98 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5, ease: "easeOut" }}
            className={cn(
                "relative overflow-hidden rounded-2xl isolate",
                "aspect-[4/3] w-full bg-gradient-to-br from-gray-900/50 via-gray-800/40 to-gray-900/50 border border-white/10 backdrop-blur-xl",
                withHoverEffect && "cursor-pointer group",
                className
            )}
        >
            {/* 🖼️ Actual Image */}
            <motion.img
                src={src}
                alt={alt}
                onLoad={() => setIsLoaded(true)}
                initial={{ opacity: 0, scale: 1.03, filter: "blur(8px)" }}
                animate={{
                    opacity: isLoaded ? 1 : 0.5,
                    scale: isLoaded ? 1 : 1.03,
                    filter: isLoaded ? "blur(0px)" : "blur(8px)",
                }}
                transition={{ duration: 0.9, ease: "easeOut" }}
                className={cn(
                    "absolute inset-0 w-full h-full object-cover transition-transform duration-700",
                    "border border-transparent",
                    withHoverEffect &&
                    "group-hover:scale-110 group-hover:brightness-110 group-hover:saturate-150",
                    "rounded-2xl"
                )}
            />

            {/* 💫 Shimmer light sweep */}
            {withHoverEffect && (
                <motion.div
                    initial={false}
                    whileHover={{ x: "100%" }}
                    transition={{ duration: 1.2, ease: "easeInOut" }}
                    className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full group-hover:translate-x-full pointer-events-none"
                />
            )}

            {/* 🌈 Neon glow overlay */}
            <div
                className={cn(
                    "absolute inset-0 pointer-events-none rounded-2xl",
                    "bg-gradient-to-br from-cyan-400/10 via-transparent to-purple-600/10 opacity-50 group-hover:opacity-100",
                    "transition-opacity duration-500"
                )}
            />

            {/* 🧊 Soft border glow */}
            <div
                className={cn(
                    "absolute inset-0 rounded-2xl pointer-events-none border border-white/5",
                    "shadow-[0_0_25px_rgba(0,255,255,0.1)] group-hover:shadow-[0_0_35px_rgba(0,255,255,0.2)]",
                    "transition-shadow duration-500"
                )}
            />

            {/* 🌀 Skeleton shimmer while loading */}
            {!isLoaded && (
                <div className="absolute inset-0 overflow-hidden rounded-2xl animate-pulse bg-gradient-to-r from-gray-800/60 via-gray-700/40 to-gray-800/60">
                    <div className="absolute inset-0 -translate-x-full bg-gradient-to-r from-transparent via-cyan-400/10 to-transparent animate-[shimmer_2s_infinite]" />
                </div>
            )}
        </motion.div>
    )
}
