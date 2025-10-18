import { useState } from "react"
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import { cn } from "@/lib/cn"

export default function FollowButton({ followeeId, isFollowing }) {
    const qc = useQueryClient()
    const [hoverUnfollow, setHoverUnfollow] = useState(false)
    const [localFollow, setLocalFollow] = useState(isFollowing)

    // 🔵 Follow
    const follow = useMutation({
        mutationFn: async () => api.post(`/api/users/${followeeId}/follow`),
        onSuccess: () => {
            toast.success("✨ Now following!")
            setLocalFollow(true)
            qc.invalidateQueries({ queryKey: ["author-id", followeeId] })
        },
        onError: () => toast.error("🚫 Failed to follow"),
    })

    // 🔴 Unfollow
    const unfollow = useMutation({
        mutationFn: async () => api.delete(`/api/users/${followeeId}/follow`),
        onSuccess: () => {
            toast.info("👋 Unfollowed")
            setLocalFollow(false)
            qc.invalidateQueries({ queryKey: ["author-id", followeeId] })
        },
        onError: () => toast.error("🚫 Failed to unfollow"),
    })

    const handleClick = () => {
        if (localFollow) unfollow.mutate()
        else follow.mutate()
    }

    return (
        <button
            onClick={handleClick}
            onMouseEnter={() => setHoverUnfollow(true)}
            onMouseLeave={() => setHoverUnfollow(false)}
            disabled={follow.isPending || unfollow.isPending}
            className={cn(
                "relative flex items-center gap-2 px-6 py-2 rounded-xl font-semibold transition-all duration-300 overflow-hidden",
                "backdrop-blur-xl border border-white/10 hover:scale-105 active:scale-95",
                "focus:outline-none focus:ring-2 focus:ring-cyan-400/40",
                localFollow
                    ? hoverUnfollow
                        ? "text-red-300 bg-red-500/10 hover:bg-red-500/20 border-red-400/30 shadow-lg shadow-red-500/20"
                        : "text-white bg-gradient-to-r from-cyan-400 to-purple-600 hover:from-cyan-500 hover:to-purple-700 shadow-cyan-500/30"
                    : "text-white bg-gradient-to-r from-cyan-400 to-purple-600 hover:from-cyan-500 hover:to-purple-700 shadow-cyan-500/30"
            )}
        >
            {/* Glow background effect */}
            <div
                className={cn(
                    "absolute inset-0 blur-lg opacity-50 transition-all duration-500",
                    localFollow
                        ? hoverUnfollow
                            ? "bg-red-500/40"
                            : "bg-cyan-500/30"
                        : "bg-cyan-500/30"
                )}
            />

            {/* Button label */}
            <span className="relative z-10 flex items-center gap-2 text-sm tracking-wide">
        {follow.isPending || unfollow.isPending ? (
            <>
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                <span>Loading…</span>
            </>
        ) : localFollow ? (
            hoverUnfollow ? (
                <>
                    🚫 <span>Unfollow?</span>
                </>
            ) : (
                <>
                    💫 <span>Following</span>
                </>
            )
        ) : (
            <>
                ✨ <span>Follow</span>
            </>
        )}
      </span>
        </button>
    )
}
