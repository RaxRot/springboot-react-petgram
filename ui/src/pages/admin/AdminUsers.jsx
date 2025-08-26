import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import { confirmToast } from "@/components/ui/Confirm"

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
            toast.success("User banned")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("Failed to ban"),
    })

    const unban = useMutation({
        mutationFn: async (id) => api.patch(`/api/admin/users/unban/${id}`),
        onSuccess: () => {
            toast.success("User unbanned")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("Failed to unban"),
    })

    const del = useMutation({
        mutationFn: async (id) => api.delete(`/api/admin/users/${id}`),
        onSuccess: () => {
            toast.success("User deleted")
            qc.invalidateQueries({ queryKey: ["adminUsers"] })
        },
        onError: () => toast.error("Failed to delete"),
    })

    if (listQ.isLoading) return <p>Loading users…</p>
    if (listQ.error) return <p className="text-red-500">{listQ.error.message}</p>

    return (
        <div className="space-y-4">
            <h1 className="text-2xl font-bold">Admin · Users</h1>

            <div className="overflow-x-auto">
                <table className="w-full border rounded-xl">
                    <thead className="bg-[--color-muted] text-left">
                    <tr>
                        <th className="p-3">ID</th>
                        <th className="p-3">Username</th>
                        <th className="p-3">Email</th>
                        <th className="p-3">Roles</th>
                        <th className="p-3">Banned</th>
                        <th className="p-3">Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {listQ.data.content?.map((u) => {
                        const roles = u.roles ?? [] // ожидаем массив строк (e.g. ["ROLE_USER","ROLE_ADMIN"])
                        const isAdmin = roles.includes("ROLE_ADMIN")
                        return (
                            <tr key={u.id} className="border-t">
                                <td className="p-3">{u.id}</td>
                                <td className="p-3">@{u.userName}</td>
                                <td className="p-3">{u.email}</td>
                                <td className="p-3">
                                    <div className="flex gap-2 flex-wrap">
                                        {roles.length === 0 && <span className="text-xs opacity-60">—</span>}
                                        {roles.map((r) => (
                                            <span key={r} className="text-[11px] px-2 py-0.5 rounded-full border">
                          {r.replace("ROLE_", "")}
                        </span>
                                        ))}
                                    </div>
                                </td>
                                <td className="p-3">{String(u.banned ?? false)}</td>
                                <td className="p-3">
                                    {/* Не показываем опасные действия для админов */}
                                    {!isAdmin ? (
                                        <div className="flex gap-2">
                                            {u.banned ? (
                                                <Button
                                                    variant="outline"
                                                    onClick={async () => {
                                                        const ok = await confirmToast({
                                                            title: `Unban @${u.userName}?`,
                                                            okText: "Unban",
                                                        })
                                                        if (ok) unban.mutate(u.id)
                                                    }}
                                                >
                                                    Unban
                                                </Button>
                                            ) : (
                                                <Button
                                                    variant="outline"
                                                    onClick={async () => {
                                                        const ok = await confirmToast({
                                                            title: `Ban @${u.userName}?`,
                                                            desc: "User will not be able to create posts and comment.",
                                                            okText: "Ban",
                                                        })
                                                        if (ok) ban.mutate(u.id)
                                                    }}
                                                >
                                                    Ban
                                                </Button>
                                            )}

                                            <Button
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
                                        <span className="text-xs opacity-60">admin</span>
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
