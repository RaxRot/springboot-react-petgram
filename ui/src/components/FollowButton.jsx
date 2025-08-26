import Button from "@/components/ui/Button"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import { useState } from "react"

export default function FollowButton({ followeeId }) {
    const qc = useQueryClient()
    const [isHovered, setIsHovered] = useState(false)

    const follow = useMutation({
        mutationFn: async () => api.post(`/api/users/${followeeId}/follow`),
        onSuccess: () => {
            toast.success("✅ Following")
            qc.invalidateQueries({ queryKey: ["author-id", followeeId] })
        },
        onError: () => toast.error("🚫 Failed to follow"),
    })

    const unfollow = useMutation({
        mutationFn: async () => api.delete(`/api/users/${followeeId}/follow`),
        onSuccess: () => {
            toast.info("🔔 Unfollowed")
            qc.invalidateQueries({ queryKey: ["author-id", followeeId] })
        },
        onError: () => toast.error("🚫 Failed to unfollow"),
    })

    return (
        <div className="flex gap-3 relative group">
            {/* Follow Button - Main attraction */}
            <Button
                size="sm"
                onClick={() => follow.mutate()}
                className="px-6 py-2 bg-gradient-to-r from-cyan-400 to-purple-600 hover:from-cyan-500 hover:to-purple-700 text-white font-semibold rounded-xl border-0 shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all duration-300 hover:scale-105 hover:skew-x-2"
            >
                <span className="flex items-center gap-2">
                    <span className="group-hover:animate-bounce">✨</span>
                    Follow
                    <span className="group-hover:animate-spin-slow">➕</span>
                </span>
            </Button>

            {/* Unfollow Button - Hidden gem */}
            <Button
                size="sm"
                variant="outline"
                onClick={() => unfollow.mutate()}
                onMouseEnter={() => setIsHovered(true)}
                onMouseLeave={() => setIsHovered(false)}
                className={`px-5 py-2 border border-red-400/30 bg-red-500/10 text-red-300 font-semibold rounded-xl backdrop-blur-sm transition-all duration-500 ease-out ${
                    isHovered
                        ? "bg-red-500/20 border-red-500/50 shadow-lg shadow-red-500/20 scale-110 -skew-x-2"
                        : "opacity-70 hover:opacity-100 hover:bg-red-500/15"
                }`}
            >
                <span className="flex items-center gap-2 transition-all duration-300">
                    {isHovered ? "🚫 Sure?" : "Unfollow"}
                    {isHovered && <span className="animate-pulse">⚠️</span>}
                </span>
            </Button>
        </div>
    )
}