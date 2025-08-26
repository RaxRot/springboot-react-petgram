import { cn } from "@/lib/cn"

export default function Input({ className, ...props }) {
    return (
        <input
            className={cn(
                "w-full rounded-xl border border-[--color-border] bg-white px-3 py-2 outline-none",
                "focus:ring-2 ring-[--color-primary] transition",
                className
            )}
            {...props}
        />
    )
}
