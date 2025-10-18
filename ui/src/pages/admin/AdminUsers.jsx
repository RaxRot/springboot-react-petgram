import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import { confirmToast } from "@/components/ui/Confirm"
import Skeleton from "@/components/ui/Skeleton"

export default function AdminUsers() {
    const qc = useQueryClient()

    // === Fetch users ===
    const listQ = useQuery({
        queryKey: ["adminUsers"],
        queryFn: async () =>
            (
                await api.get(
                    "/api/admin/users?pageNumber=0&pageSize=50&sortBy=userName&sortOrder=asc"
                )
            ).data,
    })

    // === Mutations ===
    const ban = useMutation({
        mutationFn: async (id) => api.patch(`/api/admin/users/ban/${id}`),
        onSuccess: () => {
            toast.success("✅ User banned")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("🚫 Failed to ban"),
    })

    const unban = useMutation({
        mutationFn: async (id) => api.patch(`/api/admin/users/unban/${id}`),
        onSuccess: () => {
            toast.success("✅ User unbanned")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("🚫 Failed to unban"),
    })

    const del = useMutation({
        mutationFn: async (id) => api.delete(`/api/admin/users/${id}`),
        onSuccess: () => {
            toast.success("🗑️ User deleted")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("🚫 Failed to delete"),
    })

    // === Loading skeleton ===
    if (listQ.isLoading)
        return (
            <div className="max-w-6xl mx-auto space-y-6 p-6">
                <Skeleton className="h-10 w-64" variant="text" />
                <div className="space-y-3">
                    {[...Array(5)].map((_, i) => (
                        <Skeleton
                            key={i}
                            className="h-20 w-full rounded-2xl"
                            variant="card"
                        />
                    ))}
                </div>
            </div>
        )

    // === Error ===
    if (listQ.error)
        return (
            <div className="max-w-6xl mx-auto p-6">
                <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                    Error: {listQ.error.message}
                </div>
            </div>
        )

    // === Table ===
    return (
        <div className="relative max-w-6xl mx-auto p-6 space-y-8">
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    Admin · Users
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] mt-2">
                    Manage all registered accounts
                </p>
            </div>

            <div
                className="rounded-3xl overflow-hidden border border-[hsl(var(--border))]
                    bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                    backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
                    transition-all duration-300 overflow-x-auto"
            >
                <table className="min-w-full border-collapse">
                    <thead className="bg-[hsl(var(--muted))]/20">
                    <tr>
                        {["ID", "Username", "Email", "Roles", "Banned", "Actions"].map(
                            (header) => (
                                <th
                                    key={header}
                                    className="p-4 text-left text-sm font-semibold text-[hsl(var(--foreground))] border-b border-[hsl(var(--border))]"
                                >
                                    {header}
                                </th>
                            )
                        )}
                    </tr>
                    </thead>

                    <tbody>
                    {listQ.data.content?.map((u) => {
                        const roles = u.roles ?? []
                        const isAdmin = roles.includes("ROLE_ADMIN")
                        const isBanned = u.banned ?? false

                        return (
                            <tr
                                key={u.id}
                                className="border-b border-[hsl(var(--border))]
                                        hover:bg-[hsl(var(--muted))]/10 transition-colors duration-200"
                            >
                                <td className="p-4 text-sm font-mono text-[hsl(var(--muted-foreground))]">
                                    {u.id}
                                </td>
                                <td className="p-4 text-sm font-semibold text-[hsl(var(--foreground))]">
                                    @{u.userName}
                                </td>
                                <td className="p-4 text-sm text-[hsl(var(--muted-foreground))]">
                                    {u.email}
                                </td>

                                {/* Roles */}
                                <td className="p-4">
                                    <div className="flex gap-2 flex-wrap">
                                        {roles.length === 0 && (
                                            <span className="text-xs text-[hsl(var(--muted-foreground))]">
                                                    —
                                                </span>
                                        )}
                                        {roles.map((r) => (
                                            <span
                                                key={r}
                                                className="text-[11px] px-2 py-1 rounded-full border border-cyan-400/30 bg-cyan-400/10 text-cyan-500 dark:text-cyan-300"
                                            >
                                                    {r.replace("ROLE_", "")}
                                                </span>
                                        ))}
                                    </div>
                                </td>

                                {/* Banned */}
                                <td className="p-4">
                                        <span
                                            className={`text-sm font-semibold ${
                                                isBanned
                                                    ? "text-red-500 dark:text-red-400"
                                                    : "text-green-500 dark:text-green-400"
                                            }`}
                                        >
                                            {String(isBanned)}
                                        </span>
                                </td>

                                {/* Actions */}
                                <td className="p-4">
                                    {!isAdmin ? (
                                        <div className="flex gap-2 flex-wrap">
                                            {isBanned ? (
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={async () => {
                                                        const ok =
                                                            await confirmToast({
                                                                title: `Unban @${u.userName}?`,
                                                                okText: "Unban",
                                                            })
                                                        if (ok) unban.mutate(u.id)
                                                    }}
                                                    className="bg-green-500/10 border-green-500/30 text-green-500 dark:text-green-300 hover:bg-green-500/20"
                                                >
                                                    Unban
                                                </Button>
                                            ) : (
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={async () => {
                                                        const ok =
                                                            await confirmToast({
                                                                title: `Ban @${u.userName}?`,
                                                                desc: "User will not be able to create posts or comment.",
                                                                okText: "Ban",
                                                            })
                                                        if (ok) ban.mutate(u.id)
                                                    }}
                                                    className="bg-yellow-500/10 border-yellow-500/30 text-yellow-500 dark:text-yellow-300 hover:bg-yellow-500/20"
                                                >
                                                    Ban
                                                </Button>
                                            )}

                                            <Button
                                                size="sm"
                                                variant="danger"
                                                onClick={async () => {
                                                    const ok = await confirmToast({
                                                        title: `Delete @${u.userName}?`,
                                                        desc: "All their posts, likes, and bookmarks will be removed.",
                                                        okText: "Delete",
                                                    })
                                                    if (ok) del.mutate(u.id)
                                                }}
                                            >
                                                Delete
                                            </Button>
                                        </div>
                                    ) : (
                                        <span className="text-xs text-cyan-400 opacity-80">
                                                protected
                                            </span>
                                    )}
                                </td>
                            </tr>
                        )
                    })}
                    </tbody>
                </table>
            </div>

            {/* Background glow */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    )
}
