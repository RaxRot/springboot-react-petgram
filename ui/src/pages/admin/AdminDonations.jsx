import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios"
import Skeleton from "@/components/ui/Skeleton"
import { toast } from "sonner"
import { Link } from "react-router-dom"

export default function AdminDonations() {
    const { data, isLoading, isError } = useQuery({
        queryKey: ["adminDonations"],
        queryFn: async () => (await api.get("/api/admin/users/donations")).data,
    })

    if (isLoading)
        return (
            <div className="max-w-5xl mx-auto p-6 space-y-4">
                <h2 className="text-2xl font-bold text-cyan-400 flex items-center gap-2">
                    💰 Loading donations...
                </h2>
                {[...Array(4)].map((_, i) => (
                    <Skeleton key={i} className="h-16 w-full rounded-2xl" variant="card" />
                ))}
            </div>
        )

    if (isError) {
        toast.error("Failed to load donations")
        return (
            <div className="max-w-5xl mx-auto p-6">
                <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                    Error loading donations 😿
                </div>
            </div>
        )
    }

    return (
        <div className="relative max-w-6xl mx-auto p-6 space-y-8">
            {/* Header */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    💧 Donations Analytics
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] mt-2">
                    Track all donations across the community
                </p>
            </div>

            {/* Table */}
            {data?.length === 0 ? (
                <p className="text-[hsl(var(--muted-foreground))] text-center italic">
                    No donations yet 😺
                </p>
            ) : (
                <div
                    className="rounded-3xl overflow-hidden border border-[hsl(var(--border))]
                        bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                        backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
                        transition-all duration-300 overflow-x-auto"
                >
                    <table className="min-w-full border-collapse">
                        <thead className="bg-[hsl(var(--muted))]/20">
                        <tr>
                            {["#", "Donor", "Receiver", "Amount", "Currency", "Date"].map(
                                (header) => (
                                    <th
                                        key={header}
                                        className="px-4 py-3 text-left text-sm font-semibold text-[hsl(var(--foreground))] border-b border-[hsl(var(--border))]"
                                    >
                                        {header}
                                    </th>
                                )
                            )}
                        </tr>
                        </thead>

                        <tbody>
                        {data.map((d, i) => (
                            <tr
                                key={d.id}
                                className="border-b border-[hsl(var(--border))]
                                        hover:bg-[hsl(var(--muted))]/10 transition-colors duration-200"
                            >
                                <td className="px-4 py-3 text-sm text-[hsl(var(--muted-foreground))]">
                                    {i + 1}
                                </td>

                                {/* Donor */}
                                <td className="px-4 py-3 text-sm">
                                    {d.donorUsername ? (
                                        <Link
                                            to={`/u/${encodeURIComponent(
                                                d.donorUsername
                                            )}`}
                                            className="text-cyan-400 hover:text-cyan-300 font-semibold"
                                        >
                                            @{d.donorUsername}
                                        </Link>
                                    ) : (
                                        <span className="text-[hsl(var(--muted-foreground))] italic">
                                                N/A
                                            </span>
                                    )}
                                </td>

                                {/* Receiver */}
                                <td className="px-4 py-3 text-sm">
                                    {d.receiverUsername ? (
                                        <Link
                                            to={`/u/${encodeURIComponent(
                                                d.receiverUsername
                                            )}`}
                                            className="text-purple-400 hover:text-purple-300 font-semibold"
                                        >
                                            @{d.receiverUsername}
                                        </Link>
                                    ) : (
                                        <span className="text-[hsl(var(--muted-foreground))] italic">
                                                N/A
                                            </span>
                                    )}
                                </td>

                                {/* Amount */}
                                <td className="px-4 py-3 text-sm font-medium text-[hsl(var(--foreground))]">
                                    ${Number(d.amount / 100).toFixed(2)}
                                </td>

                                {/* Currency */}
                                <td className="px-4 py-3 text-sm uppercase text-[hsl(var(--muted-foreground))]">
                                    {d.currency}
                                </td>

                                {/* Date */}
                                <td className="px-4 py-3 text-sm text-[hsl(var(--muted-foreground))] whitespace-nowrap">
                                    {new Date(d.createdAt).toLocaleString()}
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            {/* Background glow */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    )
}
