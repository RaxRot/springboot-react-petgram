// Input.jsx
import { cn } from "@/lib/cn"
import { useState } from "react"

export default function Input({ className, ...props }) {
    const [isFocused, setIsFocused] = useState(false)

    return (
        <input
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            className={cn(
                "w-full rounded-xl px-4 py-3 outline-none transition-all duration-300 ease-out",
                "bg-white dark:bg-gray-900 border border-gray-300 dark:border-gray-700",
                "text-black dark:text-white placeholder:text-gray-500 dark:placeholder:text-gray-400",
                "focus:ring-2 focus:ring-cyan-400 focus:border-cyan-400",
                "hover:border-cyan-300 dark:hover:border-cyan-600",
                "disabled:opacity-50 disabled:cursor-not-allowed",
                isFocused && "animate-glow",
                className
            )}
            {...props}
        />
    )
}