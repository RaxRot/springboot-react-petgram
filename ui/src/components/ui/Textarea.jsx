// Textarea.jsx - ИСПРАВЛЕННАЯ ВЕРСИЯ
import { cn } from "@/lib/cn"
import { useState } from "react"

export default function Textarea({ className, label, error, autoResize = true, ...props }) {
    const [isFocused, setIsFocused] = useState(false)
    const [height, setHeight] = useState("auto")

    const handleInput = (e) => {
        if (!autoResize) return
        e.target.style.height = "auto"
        e.target.style.height = e.target.scrollHeight + "px"
        setHeight(e.target.style.height)
    }

    return (
        <div className="w-full flex flex-col gap-1">
            {label && (
                <label className="text-sm font-medium text-foreground mb-1 ml-1">
                    {label}
                </label>
            )}

            <div
                className={cn(
                    "relative rounded-xl overflow-hidden transition-all duration-300 ease-out",
                    // ФИКС: убрал конфликтующие фоны
                    "bg-background border border-border",
                    "hover:border-primary/40 hover:shadow-[0_0_10px_rgba(56,189,248,0.2)]",
                    isFocused &&
                    "border-primary shadow-[0_0_20px_rgba(56,189,248,0.3)] scale-[1.01]",
                    error && "border-destructive shadow-[0_0_12px_rgba(239,68,68,0.3)]",
                    className
                )}
            >
                <textarea
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    onInput={handleInput}
                    style={{ height }}
                    className={cn(
                        "w-full resize-none px-4 py-3 text-base rounded-xl outline-none",
                        // ФИКС: нормальные цвета текста
                        "bg-transparent text-foreground placeholder:text-muted-foreground",
                        "scrollbar-thin scrollbar-thumb-primary/30 scrollbar-track-transparent",
                        "disabled:opacity-50 disabled:cursor-not-allowed"
                    )}
                    {...props}
                />

                {isFocused && (
                    <span className="absolute inset-0 rounded-xl border border-primary/50 blur-md opacity-30 animate-pulse pointer-events-none" />
                )}
            </div>

            {error && (
                <span className="text-xs text-destructive mt-1 ml-1 animate-fade-in">
                    {error}
                </span>
            )}
        </div>
    )
}