import Button from "@/components/ui/Button"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import { toast } from "sonner"

export default function FollowButton({ followeeId }) {
    const qc = useQueryClient()

    // простейшая эвристика: если подписка существует — вернём true
    // (если у тебя нет отдельного эндпоинта, можно опустить и кнопки "Подписаться/Отписаться"
    // всегда делать как действие — ниже пример без проверки состояния)
    const follow = useMutation({
        mutationFn: async () => api.post(`/api/users/${followeeId}/follow`),
        onSuccess: () => {
            toast.success("Following")
            qc.invalidateQueries({ queryKey: ["author-id", followeeId] })
        },
        onError: () => toast.error("Failed to follow"),
    })

    const unfollow = useMutation({
        mutationFn: async () => api.delete(`/api/users/${followeeId}/follow`),
        onSuccess: () => {
            toast.info("Unfollowed")
            qc.invalidateQueries({ queryKey: ["author-id", followeeId] })
        },
        onError: () => toast.error("Failed to unfollow"),
    })

    // без знания текущего состояния просто показываем “Follow” и рядом “Unfollow”
    // (если хочешь “toggle” — нужен GET-эндпоинт “isFollowing?”)
    return (
        <div className="flex gap-2">
            <Button size="sm" onClick={() => follow.mutate()}>Follow</Button>
            <Button size="sm" variant="outline" onClick={() => unfollow.mutate()}>Unfollow</Button>
        </div>
    )
}
