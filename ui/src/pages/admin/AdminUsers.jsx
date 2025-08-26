import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import { confirmToast } from "@/components/ui/Confirm"
import Skeleton from "@/components/ui/Skeleton"

export default function AdminUsers() {
    const qc = useQueryClient()

    const listQ = useQuery({
        queryKey: ["adminUsers"],
        queryFn: async () =>
            (await api.get("/api/admin/users?pageNumber=0&pageSize=50&sortBy=userName&sortOrder=asc")).data,
    })

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
            toast.success("✅ User deleted")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("🚫 Failed to delete"),
    })

    if (listQ.isLoading) return (
        <div className="space-y-6 p-6">
            <Skeleton className="h-10 w-64" variant="text"/>
            <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                    <Skeleton key={i} className="h-20 w-full rounded-2xl" variant="card"/>
                ))}
            </div>
        </div>
    )

    if (listQ.error) return (
        <div className="p-6">
            <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                Error: {listQ.error.message}
            </div>
        </div>
    )

    return (
        <div className="p-6 space-y-6">
            <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                Admin · Users
            </h1>

            <div className="bg-white dark:bg-white/5 backdrop-blur-xl border border-gray-200 dark:border-white/10 rounded-2xl overflow-hidden">
                <table className="w-full">
                    <thead className="bg-gray-100 dark:bg-white/10">
                    <tr>
                        {["ID", "Username", "Email", "Roles", "Banned", "Actions"].map((header) => (
                            <th key={header} className="p-4 text-left text-sm font-semibold text-gray-700 dark:text-gray-300 border-b border-gray-200 dark:border-white/10">
                                {header}
                            </th>
                        ))}
                    </tr>
                    </thead>
                    <tbody>
                    {listQ.data.content?.map((u) => {
                        const roles = u.roles ?? []
                        const isAdmin = roles.includes("ROLE_ADMIN")
                        const isBanned = u.banned ?? false

                        return (
                            <tr key={u.id} className="border-b border-gray-100 dark:border-white/5 hover:bg-gray-50 dark:hover:bg-white/3 transition-all duration-300">
                                <td className="p-4 text-sm text-gray-600 dark:text-gray-300 font-mono">{u.id}</td>
                                <td className="p-4 text-sm font-semibold text-gray-900 dark:text-white">@{u.userName}</td>
                                <td className="p-4 text-sm text-gray-600 dark:text-gray-400">{u.email}</td>
                                <td className="p-4">
                                    <div className="flex gap-2 flex-wrap">
                                        {roles.length === 0 && (
                                            <span className="text-xs text-gray-500 dark:text-gray-500">—</span>
                                        )}
                                        {roles.map((r) => (
                                            <span
                                                key={r}
                                                className="text-[11px] px-2 py-1 rounded-full border border-cyan-400/30 bg-cyan-400/10 text-cyan-600 dark:text-cyan-300"
                                            >
                                                    {r.replace("ROLE_", "")}
                                                </span>
                                        ))}
                                    </div>
                                </td>
                                <td className="p-4">
                                        <span className={`text-sm font-semibold ${isBanned ? 'text-red-600 dark:text-red-400' : 'text-green-600 dark:text-green-400'}`}>
                                            {String(isBanned)}
                                        </span>
                                </td>
                                <td className="p-4">
                                    {!isAdmin ? (
                                        <div className="flex gap-2">
                                            {isBanned ? (
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={async () => {
                                                        const ok = await confirmToast({
                                                            title: `Unban @${u.userName}?`,
                                                            okText: "Unban",
                                                        })
                                                        if (ok) unban.mutate(u.id)
                                                    }}
                                                    className="bg-green-500/10 border-green-500/30 text-green-600 dark:text-green-300 hover:bg-green-500/20"
                                                >
                                                    Unban
                                                </Button>
                                            ) : (
                                                <Button
                                                    size="sm"
                                                    variant="outline"
                                                    onClick={async () => {
                                                        const ok = await confirmToast({
                                                            title: `Ban @${u.userName}?`,
                                                            desc: "User will not be able to create posts and comment.",
                                                            okText: "Ban",
                                                        })
                                                        if (ok) ban.mutate(u.id)
                                                    }}
                                                    className="bg-yellow-500/10 border-yellow-500/30 text-yellow-600 dark:text-yellow-300 hover:bg-yellow-500/20"
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
                                                        desc: "All their posts, likes and bookmarks will be removed.",
                                                        okText: "Delete",
                                                    })
                                                    if (ok) del.mutate(u.id)
                                                }}
                                            >
                                                Delete
                                            </Button>
                                        </div>
                                    ) : (
                                        <span className="text-xs text-cyan-600 dark:text-cyan-400 opacity-80">protected</span>
                                    )}
                                </td>
                            </tr>
                        )
                    })}
                    </tbody>
                </table>
            </div>
        </div>
    )
}