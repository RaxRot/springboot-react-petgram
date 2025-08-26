import { useParams, Link } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/axios";
import Button from "@/components/ui/Button";
import { toast } from "sonner";
import { useAuth } from "@/store/auth";
import { useState } from "react";
import Input from "@/components/ui/Input";

import FollowButton from "@/components/FollowButton";
import { confirmToast } from "@/components/ui/Confirm";
import ResponsiveImage from "@/components/ui/ResponsiveImage";

export default function PostDetails() {
    const { id } = useParams();
    const qc = useQueryClient();
    const { user, isAdmin } = useAuth();
    const [comment, setComment] = useState("");

    // пост
    const postQ = useQuery({
        queryKey: ["post", id],
        queryFn: async () => (await api.get(`/api/public/posts/${id}`)).data,
    });

    // лайки
    const likesCountQ = useQuery({
        queryKey: ["likes", id],
        queryFn: async () => (await api.get(`/api/public/posts/${id}/likes`)).data.likesCount,
    });

    // комментарии
    const commentsQ = useQuery({
        queryKey: ["comments", id],
        queryFn: async () =>
            (await api.get(`/api/public/posts/${id}/comments?pageNumber=0&pageSize=50&sortBy=createdAt&sortOrder=asc`)).data,
    });

    // автор — получаем id по username (публичный эндпоинт)
    const authorUsername = postQ.data?.user?.userName;
    const authorIdQ = useQuery({
        queryKey: ["author-id", authorUsername],
        enabled: !!authorUsername,
        queryFn: async () => {
            const { data } = await api.get(`/api/public/users/username/${authorUsername}/id`);
            return typeof data === "number" ? data : data?.id;
        },
    });

    // мутации
    const like = useMutation({
        mutationFn: async () => api.post(`/api/posts/${id}/likes`),
        onSuccess: () => qc.invalidateQueries({ queryKey: ["likes", id] }),
    });
    const unlike = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}/likes`),
        onSuccess: () => qc.invalidateQueries({ queryKey: ["likes", id] }),
    });

    const bookmark = useMutation({
        mutationFn: async () => api.post(`/api/posts/${id}/bookmarks`),
        onSuccess: () => toast.success("Saved to bookmarks"),
    });
    const unbookmark = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}/bookmarks`),
        onSuccess: () => toast.info("Removed from bookmarks"),
    });

    const del = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}`),
        onSuccess: () => {
            toast.success("Post deleted");
            window.history.back();
        },
        onError: () => toast.error("Failed to delete post"),
    });

    const addComment = useMutation({
        mutationFn: async (text) => api.post(`/api/posts/${id}/comments`, { text }),
        onSuccess: () => {
            setComment("");
            qc.invalidateQueries({ queryKey: ["comments", id] });
        },
    });
    const deleteComment = useMutation({
        mutationFn: async (cid) => api.delete(`/api/comments/${cid}`),
        onSuccess: () => {
            toast.success("Comment removed");
            qc.invalidateQueries({ queryKey: ["comments", id] });
        },
    });

    const banUser = useMutation({
        mutationFn: async (userId) => api.patch(`/api/admin/users/ban/${userId}`),
        onSuccess: () => toast.success("User banned"),
        onError: () => toast.error("Failed to ban"),
    });

    if (postQ.isLoading) return <p>Loading...</p>;
    if (postQ.error) return <p className="text-red-500">{postQ.error.message}</p>;

    const p = postQ.data;
    const canDelete = user && (isAdmin() || user.username === p.user?.userName);

    const onDeleteClick = async () => {
        const ok = await confirmToast({
            title: "Delete this post?",
            desc: "This action cannot be undone.",
            okText: "Delete",
        });
        if (ok) del.mutate();
    };

    return (
        <div className="max-w-2xl mx-auto space-y-6">
            <div>
                <h1 className="text-2xl font-bold">{p.title}</h1>
                <div className="text-xs opacity-60">{p.animalType}</div>

                {/* Автор + переход на профиль + Follow */}
                <div className="text-sm mt-2 flex items-center gap-3">
                    <Link to={`/u/${p.user?.userName}`} className="hover:underline">
                        @{p.user?.userName ?? "user"}
                    </Link>
                    {authorIdQ.data && user && user.username !== p.user?.userName && (
                        <FollowButton followeeId={authorIdQ.data} />
                    )}
                </div>

                {/* Картинка и текст */}
                {p.imageUrl && <ResponsiveImage src={p.imageUrl} alt={p.title} className="mt-3" />}
                {p.content && <p className="mt-3">{p.content}</p>}
            </div>

            {/* Действия */}
            <div className="flex flex-wrap items-center gap-2">
                <Button
                    onClick={() => toast.promise(like.mutateAsync(), { loading: "Liking...", success: "Liked", error: "Failed" })}
                >
                    Like
                </Button>
                <Button
                    variant="outline"
                    onClick={() =>
                        toast.promise(unlike.mutateAsync(), { loading: "Unliking...", success: "Unliked", error: "Failed" })
                    }
                >
                    Unlike
                </Button>
                <span className="text-sm opacity-70">❤ {likesCountQ.data ?? 0}</span>

                <Button
                    className="ml-2"
                    onClick={() =>
                        toast.promise(bookmark.mutateAsync(), { loading: "Saving...", success: "Saved", error: "Failed" })
                    }
                >
                    Bookmark
                </Button>
                <Button
                    variant="outline"
                    onClick={() =>
                        toast.promise(unbookmark.mutateAsync(), { loading: "Removing...", success: "Removed", error: "Failed" })
                    }
                >
                    Unbookmark
                </Button>

                {canDelete && (
                    <Button variant="danger" className="ml-auto" onClick={onDeleteClick}>
                        Delete
                    </Button>
                )}
            </div>

            {/* Комментарии */}
            <section className="space-y-3">
                <h2 className="text-xl font-semibold">Comments</h2>

                {commentsQ.data?.content?.map((c) => (
                    <div key={c.id} className="rounded-xl border p-3 flex items-start justify-between gap-3">
                        <div>
                            <div className="text-xs opacity-60">@{c.author?.userName ?? "user"}</div>
                            <p className="mt-1">{c.text}</p>
                        </div>

                        <div className="flex items-center gap-2">
                            {user && (isAdmin() || user.id === c.author?.id) && (
                                <Button variant="outline" onClick={() => deleteComment.mutate(c.id)} className="px-3 py-1">
                                    Delete
                                </Button>
                            )}

                            {isAdmin() && c.author?.id && (
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={async () => {
                                        const ok = await confirmToast({
                                            title: `Ban @${c.author?.userName}?`,
                                            desc: "The user will lose posting/commenting ability.",
                                            okText: "Ban",
                                        });
                                        if (ok) banUser.mutate(c.author.id);
                                    }}
                                >
                                    Ban
                                </Button>
                            )}
                        </div>
                    </div>
                ))}

                {user && (
                    <form
                        onSubmit={(e) => {
                            e.preventDefault();
                            if (comment.trim()) addComment.mutate(comment.trim());
                        }}
                        className="flex gap-2"
                    >
                        <Input
                            placeholder="Write a comment…"
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                        />
                        <Button type="submit">Send</Button>
                    </form>
                )}
            </section>
        </div>
    );
}
