import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/axios";
import { Link } from "react-router-dom";

export default function Bookmarks() {
    const q = useQuery({
        queryKey: ["bookmarks"],
        queryFn: async () =>
            (await api.get("/api/user/bookmarks?pageNumber=0&pageSize=20&sortBy=createdAt&sortOrder=desc")).data,
    });

    if (q.isLoading) return <p>Loading...</p>;
    if (q.error) return <p className="text-red-500">{q.error.message}</p>;

    return (
        <div className="grid gap-4">
            <h1 className="text-2xl font-bold">My Bookmarks</h1>

            {q.data.content?.length === 0 && <p>No bookmarks yet.</p>}

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {q.data.content?.map((p) => (
                    <Link
                        key={p.id}
                        to={`/posts/${p.id}`}
                        className="rounded-2xl border p-4 hover:bg-[--color-muted] transition"
                    >
                        <div className="text-xs opacity-60">{p.animalType}</div>
                        <h3 className="text-lg font-semibold line-clamp-2">{p.title}</h3>

                        {p.imageUrl && (
                            <div className="mt-3 rounded-xl overflow-hidden">
                                <img src={p.imageUrl} alt={p.title} className="block w-full h-auto" />
                            </div>
                        )}
                    </Link>
                ))}
            </div>
        </div>
    );
}
