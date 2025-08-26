import { cn } from "@/lib/cn"
export default function Textarea({ className, ...props }) {
    return (
        <textarea
            className={cn(
                "w-full min-h-28 rounded-xl border border-[--color-border] bg-white px-3 py-2 outline-none",
                "focus:ring-2 ring-[--color-primary] transition",
                className
            )}
            {...props}
        />
    )
}
