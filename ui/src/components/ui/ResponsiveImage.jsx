import { useState } from "react"
import { cn } from "@/lib/cn"

export default function ResponsiveImage({ src, alt = "", className = "", withHoverEffect = true }) {
    const [isLoaded, setIsLoaded] = useState(false)

    return (
        <div className={cn(
            "relative group overflow-hidden",
            withHoverEffect && "cursor-pointer",
            // Fixed aspect ratio container
            "aspect-[4/3] w-full",
            className
        )}>
            {/* Main image with fixed size and cropping */}
            <img
                src={src}
                alt={alt}
                onLoad={() => setIsLoaded(true)}
                className={cn(
                    "absolute inset-0 w-full h-full object-cover transition-all duration-700 ease-out",
                    // Base styles
                    "border border-white/10 shadow-lg shadow-cyan-500/10",
                    // Loading state
                    !isLoaded && "blur-sm scale-95 opacity-80",
                    isLoaded && "blur-0 scale-100 opacity-100",
                    // Hover effects
                    withHoverEffect && [
                        "group-hover:scale-110",
                        "group-hover:shadow-cyan-500/20",
                        "group-hover:border-cyan-400/30",
                        "group-hover:shadow-xl"
                    ]
                )}
            />

            {/* Shine effect on hover */}
            {withHoverEffect && (
                <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent
                              -translate-x-full group-hover:translate-x-full transition-transform duration-1000 ease-in-out
                              rounded-2xl pointer-events-none" />
            )}

            {/* Subtle glow overlay */}
            <div className="absolute inset-0 bg-gradient-to-br from-cyan-400/5 via-transparent to-purple-600/5
                          rounded-2xl pointer-events-none opacity-70 group-hover:opacity-100
                          transition-opacity duration-500" />

            {/* Loading skeleton */}
            {!isLoaded && (
                <div className="absolute inset-0 bg-gradient-to-r from-gray-800 to-gray-900
                              rounded-2xl animate-pulse-slow" />
            )}
        </div>
    )
}