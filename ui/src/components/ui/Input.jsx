// Input.jsx - ИСПРАВЛЕННАЯ ВЕРСИЯ
import { cn } from "@/lib/cn"
import { useState } from "react"

export default function Input({ className, label, error, icon: Icon, ...props }) {
    const [isFocused, setIsFocused] = useState(false)

    return (
        <div className="w-full flex flex-col gap-1">
            {label && (
                // ФИКС: убрал хардкод цвета
                <label className="text-sm font-medium text-foreground mb-1 ml-1">
                    {label}
                </label>
            )}

            <div
                className={cn(
                    "relative rounded-xl overflow-hidden transition-all duration-300 ease-out",
                    // ФИКС: нормальный фон без конфликтов
                    "bg-background border border-border",
                    "hover:border-primary/40 hover:shadow-[0_0_10px_rgba(56,189,248,0.2)]",
                    isFocused &&
                    "border-primary shadow-[0_0_15px_rgba(56,189,248,0.3)] scale-[1.01]",
                    error && "border-destructive shadow-[0_0_12px_rgba(239,68,68,0.3)]",
                    className
                )}
            >
                {Icon && (
                    <span className="absolute left-3 top-1/2 -translate-y-1/2 text-primary">
                        <Icon className="w-5 h-5" />
                    </span>
                )}

                <input
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    className={cn(
                        "w-full px-4 py-3 text-base rounded-xl outline-none bg-transparent",
                        Icon && "pl-10",
                        // ФИКС: используем CSS переменные вместо хардкода
                        "text-foreground placeholder:text-muted-foreground",
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