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
import Skeleton from "@/components/ui/Skeleton";

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
        onSuccess: () => toast.success("✅ Saved to bookmarks"),
    });
    const unbookmark = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}/bookmarks`),
        onSuccess: () => toast.info("🔔 Removed from bookmarks"),
    });

    const del = useMutation({
        mutationFn: async () => api.delete(`/api/posts/${id}`),
        onSuccess: () => {
            toast.success("🗑️ Post deleted");
            window.history.back();
        },
        onError: () => toast.error("🚫 Failed to delete post"),
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
            toast.success("🗑️ Comment removed");
            qc.invalidateQueries({ queryKey: ["comments", id] });
        },
    });

    const banUser = useMutation({
        mutationFn: async (userId) => api.patch(`/api/admin/users/ban/${userId}`),
        onSuccess: () => toast.success("🔨 User banned"),
        onError: () => toast.error("🚫 Failed to ban"),
    });

    if (postQ.isLoading) return (
        <div className="max-w-2xl mx-auto p-6 space-y-6">
            <Skeleton className="h-10 w-3/4" variant="text"/>
            <Skeleton className="h-4 w-32" variant="text"/>
            <Skeleton className="h-96 w-full rounded-2xl" variant="image"/>
            <div className="flex gap-2 flex-wrap">
                <Skeleton className="h-10 w-20 rounded-xl" variant="button"/>
                <Skeleton className="h-10 w-20 rounded-xl" variant="button"/>
                <Skeleton className="h-10 w-20 rounded-xl" variant="button"/>
            </div>
        </div>
    );

    if (postQ.error) return (
        <div className="max-w-2xl mx-auto p-6">
            <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 text-red-600 dark:text-red-300">
                Error: {postQ.error.message}
            </div>
        </div>
    );

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
        <div className="max-w-2xl mx-auto p-6 space-y-6">
            {/* Header */}
            <div className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-6 border border-gray-200 dark:border-white/10">
                <h1 className="text-3xl font-bold text-gray-900 dark:text-white">{p.title}</h1>
                <div className="text-sm text-cyan-600 dark:text-cyan-400 font-medium mt-1">
                    {p.animalType}
                </div>

                {/* Author info */}
                <div className="flex items-center gap-3 mt-4">
                    <Link
                        to={`/u/${p.user?.userName}`}
                        className="flex items-center gap-2 group"
                    >
                        <div className="w-10 h-10 bg-gradient-to-r from-cyan-400 to-purple-600 rounded-full flex items-center justify-center text-white font-bold">
                            {p.user?.userName?.[0]?.toUpperCase()}
                        </div>
                        <span className="text-gray-700 dark:text-gray-300 group-hover:text-cyan-600 dark:group-hover:text-cyan-400 transition-colors">
                            @{p.user?.userName ?? "user"}
                        </span>
                    </Link>

                    {authorIdQ.data && user && user.username !== p.user?.userName && (
                        <FollowButton followeeId={authorIdQ.data} />
                    )}
                </div>

                {/* Content */}
                {p.content && (
                    <p className="mt-4 text-gray-700 dark:text-gray-300 leading-relaxed">
                        {p.content}
                    </p>
                )}
            </div>

            {/* Image */}
            {p.imageUrl && (
                <div className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl overflow-hidden border border-gray-200 dark:border-white/10">
                    <ResponsiveImage src={p.imageUrl} alt={p.title} />
                </div>
            )}

            {/* Actions */}
            <div className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-4 border border-gray-200 dark:border-white/10">
                <div className="flex flex-wrap items-center gap-3">
                    <Button
                        onClick={() => like.mutate()}
                        className="flex items-center gap-2"
                        variant="outline"
                    >
                        <span>❤️</span>
                        Like
                    </Button>

                    <Button
                        onClick={() => unlike.mutate()}
                        variant="ghost"
                        className="flex items-center gap-2"
                    >
                        <span>💔</span>
                        Unlike
                    </Button>

                    <span className="text-lg font-semibold text-cyan-600 dark:text-cyan-400">
                        ❤ {likesCountQ.data ?? 0}
                    </span>

                    <div className="flex-1"></div>

                    <Button
                        onClick={() => bookmark.mutate()}
                        className="flex items-center gap-2"
                        variant="outline"
                    >
                        <span>🔖</span>
                        Save
                    </Button>

                    <Button
                        onClick={() => unbookmark.mutate()}
                        variant="ghost"
                        className="flex items-center gap-2"
                    >
                        <span>📑</span>
                        Unsave
                    </Button>

                    {canDelete && (
                        <Button
                            variant="danger"
                            onClick={onDeleteClick}
                            className="flex items-center gap-2"
                        >
                            <span>🗑️</span>
                            Delete
                        </Button>
                    )}
                </div>
            </div>

            {/* Comments */}
            <section className="bg-white dark:bg-white/5 backdrop-blur-sm rounded-2xl p-6 border border-gray-200 dark:border-white/10">
                <h2 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Comments 💬</h2>

                {commentsQ.data?.content?.map((c) => (
                    <div key={c.id} className="bg-gray-50 dark:bg-white/10 rounded-2xl p-4 mb-3 border border-gray-200 dark:border-white/10">
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1">
                                <div className="flex items-center gap-2 mb-2">
                                    <div className="w-8 h-8 bg-gradient-to-r from-cyan-400 to-purple-600 rounded-full flex items-center justify-center text-white text-xs font-bold">
                                        {c.author?.userName?.[0]?.toUpperCase()}
                                    </div>
                                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                                        @{c.author?.userName ?? "user"}
                                    </span>
                                </div>
                                <p className="text-gray-800 dark:text-gray-200">{c.text}</p>
                            </div>

                            <div className="flex items-center gap-2">
                                {user && (isAdmin() || user.id === c.author?.id) && (
                                    <Button
                                        variant="outline"
                                        onClick={() => deleteComment.mutate(c.id)}
                                        className="px-3 py-1 text-sm"
                                        size="sm"
                                    >
                                        🗑️
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
                                        className="px-3 py-1 text-sm"
                                    >
                                        🔨
                                    </Button>
                                )}
                            </div>
                        </div>
                    </div>
                ))}

                {user && (
                    <form
                        onSubmit={(e) => {
                            e.preventDefault();
                            if (comment.trim()) addComment.mutate(comment.trim());
                        }}
                        className="flex gap-3 mt-6"
                    >
                        <Input
                            placeholder="Write your comment…"
                            value={comment}
                            onChange={(e) => setComment(e.target.value)}
                            className="flex-1"
                        />
                        <Button
                            type="submit"
                            disabled={!comment.trim()}
                            className="px-6"
                        >
                            💬 Send
                        </Button>
                    </form>
                )}
            </section>
        </div>
    );
}