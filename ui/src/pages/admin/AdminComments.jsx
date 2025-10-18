import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import { confirmToast } from "@/components/ui/Confirm"
import Skeleton from "@/components/ui/Skeleton"

export default function AdminComments() {
    const qc = useQueryClient()

    // === Load all comments ===
    const listQ = useQuery({
        queryKey: ["adminComments"],
        queryFn: async () =>
            (
                await api.get(
                    "/api/admin/comments?pageNumber=0&pageSize=50&sortBy=createdAt&sortOrder=desc"
                )
            ).data,
    })

    // === Delete comment ===
    const del = useMutation({
        mutationFn: async (id) => api.delete(`/api/comments/${id}`),
        onSuccess: () => {
            toast.success("🗑️ Comment deleted")
            qc.invalidateQueries({ queryKey: ["adminComments"] })
        },
        onError: () => toast.error("🚫 Failed to delete comment"),
    })

    // === Loading state ===
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

    // === Error state ===
    if (listQ.error)
        return (
            <div className="max-w-6xl mx-auto p-6">
                <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                    Error: {listQ.error.message}
                </div>
            </div>
        )

    const comments = listQ.data.content ?? []

    return (
        <div className="max-w-6xl mx-auto p-6 space-y-8">
            {/* Header */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    Admin · Comments
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] mt-2">
                    Manage and moderate all user comments
                </p>
            </div>

            {/* Table */}
            <div
                className="rounded-3xl overflow-hidden border border-[hsl(var(--border))]
                    bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                    backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
                    transition-all duration-300"
            >
                <table className="w-full border-collapse">
                    <thead className="bg-[hsl(var(--muted))]/20">
                    <tr>
                        {["ID", "Author", "Text", "Created", "Actions"].map(
                            (header) => (
                                <th
                                    key={header}
                                    className="p-4 text-left text-sm font-semibold text-[hsl(var(--foreground))]
                                            border-b border-[hsl(var(--border))]"
                                >
                                    {header}
                                </th>
                            )
                        )}
                    </tr>
                    </thead>

                    <tbody>
                    {comments.map((c) => (
                        <tr
                            key={c.id}
                            className="border-b border-[hsl(var(--border))]
                                    hover:bg-[hsl(var(--muted))]/10 transition-colors duration-200"
                        >
                            <td className="p-4 text-sm font-mono text-[hsl(var(--muted-foreground))]">
                                {c.id}
                            </td>

                            <td className="p-4 text-sm font-semibold text-[hsl(var(--foreground))]">
                                @{c.author?.userName ?? "unknown"}
                            </td>

                            <td className="p-4 text-sm text-[hsl(var(--foreground))] max-w-xs truncate">
                                {c.text || "—"}
                            </td>

                            <td className="p-4 text-sm text-[hsl(var(--muted-foreground))] whitespace-nowrap">
                                {new Date(c.createdAt ?? "").toLocaleString() || "—"}
                            </td>

                            <td className="p-4">
                                <Button
                                    size="sm"
                                    variant="danger"
                                    onClick={async () => {
                                        const ok = await confirmToast({
                                            title: "Delete this comment?",
                                            desc: `By @${
                                                c.author?.userName ?? "user"
                                            }`,
                                            okText: "Delete",
                                        })
                                        if (ok) del.mutate(c.id)
                                    }}
                                >
                                    Delete
                                </Button>
                            </td>
                        </tr>
                    ))}

                    {comments.length === 0 && (
                        <tr>
                            <td
                                colSpan={5}
                                className="text-center p-8 text-[hsl(var(--muted-foreground))]"
                            >
                                No comments yet 🎧
                            </td>
                        </tr>
                    )}
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
