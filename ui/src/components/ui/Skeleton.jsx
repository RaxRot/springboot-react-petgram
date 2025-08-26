import { cn } from "@/lib/cn"
export default function Skeleton({ className }) {
    return <div className={cn("animate-pulse rounded-xl bg-[--color-muted]", className)} />
}
