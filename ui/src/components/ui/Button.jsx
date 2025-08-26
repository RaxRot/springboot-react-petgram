import { cn } from "@/lib/cn"

export default function Button({ as:Comp="button", className, variant="primary", ...props }) {
    const styles = {
        primary: "bg-[--color-primary] text-[--color-primary-foreground] hover:opacity-90",
        ghost: "bg-transparent hover:bg-[--color-muted]",
        danger: "bg-red-600 text-white hover:opacity-90",
        outline: "border border-[--color-border] bg-white hover:bg-[--color-muted]",
    }
    return (
        <Comp
            className={cn(
                "inline-flex items-center justify-center rounded-2xl px-4 py-2 text-sm font-medium transition active:scale-[.98] disabled:opacity-50",
                styles[variant],
                className
            )}
            {...props}
        />
    )
}
