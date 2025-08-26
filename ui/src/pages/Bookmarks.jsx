import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/axios";
import { Link } from "react-router-dom";
import Skeleton from "@/components/ui/Skeleton";
import ResponsiveImage from "@/components/ui/ResponsiveImage";

export default function Bookmarks() {
    const q = useQuery({
        queryKey: ["bookmarks"],
        queryFn: async () =>
            (await api.get("/api/user/bookmarks?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc")).data,
    });

    if (q.isLoading) return (
        <div className="max-w-7xl mx-auto p-6 space-y-6">
            <Skeleton className="h-10 w-64" variant="text"/>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                {[...Array(8)].map((_, i) => (
                    <div key={i} className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10">
                        <Skeleton className="h-4 w-20 mb-3" variant="text"/>
                        <Skeleton className="h-6 w-full mb-4" variant="text"/>
                        <Skeleton className="h-48 w-full rounded-xl" variant="image"/>
                    </div>
                ))}
            </div>
        </div>
    );

    if (q.error) return (
        <div className="max-w-7xl mx-auto p-6">
            <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                Error: {q.error.message}
            </div>
        </div>
    );

    return (
        <div className="max-w-7xl mx-auto p-6 space-y-6">
            {/* Header */}
            <div className="text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    My Bookmarks
                </h1>
                <p className="text-gray-600 dark:text-gray-400 mt-2">Your saved pet moments</p>
            </div>

            {q.data.content?.length === 0 ? (
                <div className="text-center py-16">
                    <div className="text-6xl mb-4">🔖</div>
                    <p className="text-gray-600 dark:text-gray-400 text-lg">No bookmarks yet</p>
                    <p className="text-gray-500 dark:text-gray-500 text-sm">Start saving your favorite posts!</p>
                </div>
            ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
                    {q.data.content?.map((p) => (
                        <Link
                            key={p.id}
                            to={`/posts/${p.id}`}
                            className="block bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10
                                     hover:bg-gray-50 dark:hover:bg-white/10 hover:border-cyan-400/30 hover:shadow-lg hover:shadow-cyan-500/10
                                     transition-all duration-300 group"
                        >
                            {/* Animal type badge */}
                            <div className="text-xs font-semibold text-cyan-600 dark:text-cyan-400 mb-2">
                                {p.animalType}
                            </div>

                            {/* Title */}
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white line-clamp-2 group-hover:text-cyan-600 dark:group-hover:text-cyan-400 transition-colors">
                                {p.title}
                            </h3>

                            {/* Author */}
                            <div className="text-sm text-gray-600 dark:text-gray-400 mt-2">
                                by @{p.user?.userName ?? "unknown"}
                            </div>

                            {/* Image */}
                            {p.imageUrl && (
                                <div className="mt-3 rounded-xl overflow-hidden">
                                    <ResponsiveImage
                                        src={p.imageUrl}
                                        alt={p.title}
                                        className="group-hover:scale-105 transition-transform duration-300"
                                    />
                                </div>
                            )}

                            {/* Bookmark indicator */}
                            <div className="absolute top-3 right-3 w-8 h-8 bg-gradient-to-r from-cyan-400 to-purple-600 rounded-full
                                         flex items-center justify-center text-white text-sm opacity-0 group-hover:opacity-100
                                         transition-opacity duration-300">
                                🔖
                            </div>
                        </Link>
                    ))}
                </div>
            )}

            {/* Background elements */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    );
}