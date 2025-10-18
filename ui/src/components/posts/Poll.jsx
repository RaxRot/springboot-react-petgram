import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/axios";
import { toast } from "sonner";
import Button from "@/components/ui/Button";
import Input from "@/components/ui/Input";
import { useAuth } from "@/store/auth";
import { useState } from "react";

export default function Poll({ postId, postAuthorUsername }) {
    const qc = useQueryClient();
    const { user } = useAuth();

    const [showForm, setShowForm] = useState(false);
    const [question, setQuestion] = useState("");
    const [options, setOptions] = useState(["", ""]);

    // === получить опрос ===
    const pollQ = useQuery({
        queryKey: ["poll", postId],
        queryFn: async () => {
            try {
                const { data } = await api.get(`/api/posts/${postId}/polls`);
                return data;
            } catch (e) {
                if (e.response?.status === 404) return null;
                throw e;
            }
        },
        retry: false,
        refetchOnWindowFocus: false,
    });

    // === создать опрос ===
    const createPoll = useMutation({
        mutationFn: async () =>
            (await api.post(`/api/posts/${postId}/polls`, { question, options })).data,
        onSuccess: (data) => {
            toast.success("🗳️ Poll created!");
            qc.setQueryData(["poll", postId], data);
            setShowForm(false);
            setQuestion("");
            setOptions(["", ""]);
        },
        onError: () => toast.error("Failed to create poll"),
    });

    // === удалить опрос ===
    const deletePoll = useMutation({
        mutationFn: async () => await api.delete(`/api/posts/${postId}/polls`),
        onSuccess: () => {
            toast.success("🗑️ Poll deleted");
            qc.setQueryData(["poll", postId], null);
        },
        onError: () => toast.error("Failed to delete poll"),
    });

    // === голосовать ===
    const voteM = useMutation({
        mutationFn: async (optionId) =>
            (await api.post(`/api/polls/${pollQ.data.pollId}/vote/${optionId}`)).data,
        onSuccess: (data) => {
            toast.success("✅ Vote counted!");
            qc.setQueryData(["poll", postId], data);
        },
        onError: (e) => {
            const msg = e?.response?.data?.message;
            if (msg !== "You already voted") {
                toast.error(msg || "Voting failed");
            }
        },
    });

    if (pollQ.isLoading) return <p className="text-gray-400">Loading poll...</p>;

    // === если опроса нет ===
    if (!pollQ.data) {
        const isAuthor = user && user.username === postAuthorUsername;

        return (
            <div className="p-6 mt-4 rounded-2xl border border-[hsl(var(--border))] bg-[hsl(var(--card))]/70 backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.08)] transition-all">
                {!showForm ? (
                    <div className="text-center">
                        <p className="text-[hsl(var(--muted-foreground))] mb-3">
                            No poll yet for this post.
                        </p>
                        {isAuthor && (
                            <Button
                                onClick={() => setShowForm(true)}
                                className="bg-gradient-to-r from-cyan-400 to-purple-600"
                            >
                                ➕ Create Poll
                            </Button>
                        )}
                    </div>
                ) : (
                    <div className="space-y-4">
                        <Input
                            placeholder="Poll question"
                            value={question}
                            onChange={(e) => setQuestion(e.target.value)}
                            className="w-full"
                        />
                        {options.map((opt, i) => (
                            <Input
                                key={i}
                                placeholder={`Option ${i + 1}`}
                                value={opt}
                                onChange={(e) =>
                                    setOptions(
                                        options.map((v, idx) =>
                                            idx === i ? e.target.value : v
                                        )
                                    )
                                }
                                className="w-full"
                            />
                        ))}
                        <div className="flex justify-between">
                            <Button
                                onClick={() => setOptions([...options, ""])}
                                variant="outline"
                            >
                                ➕ Add option
                            </Button>
                            <Button
                                onClick={() => {
                                    const valid =
                                        question.trim() &&
                                        options.filter(Boolean).length >= 2;
                                    if (!valid)
                                        return toast.error(
                                            "At least 2 options required"
                                        );
                                    createPoll.mutate();
                                }}
                                disabled={createPoll.isPending}
                                className="bg-gradient-to-r from-cyan-400 to-purple-600"
                            >
                                {createPoll.isPending ? "Saving..." : "Save Poll"}
                            </Button>
                        </div>
                    </div>
                )}
            </div>
        );
    }

    // === если опрос есть ===
    const poll = pollQ.data;
    const totalVotes = poll.options.reduce((a, o) => a + o.votes, 0);
    const isAuthor = user && user.username === postAuthorUsername;

    return (
        <div className="p-6 mt-4 rounded-2xl border border-[hsl(var(--border))] bg-[hsl(var(--card))]/70 backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.08)] transition-all">
            <div className="flex justify-between items-center mb-3">
                <h4 className="font-semibold text-lg text-[hsl(var(--foreground))]">
                    {poll.question}
                </h4>
                {isAuthor && (
                    <Button
                        variant="danger"
                        onClick={() => deletePoll.mutate()}
                        disabled={deletePoll.isPending}
                    >
                        🗑 Delete Poll
                    </Button>
                )}
            </div>

            <div className="space-y-2">
                {poll.options.map((opt) => {
                    const percentage =
                        totalVotes > 0
                            ? Math.round((opt.votes / totalVotes) * 100)
                            : 0;

                    return (
                        <div
                            key={opt.id}
                            className={`relative p-3 rounded-xl border border-[hsl(var(--border))] ${
                                poll.voted
                                    ? "cursor-not-allowed opacity-70"
                                    : "hover:border-cyan-400/50"
                            } transition-all`}
                        >
                            <div
                                className="absolute left-0 top-0 h-full bg-cyan-400/20 rounded-xl transition-all duration-500"
                                style={{ width: `${percentage}%` }}
                            />
                            <button
                                disabled={poll.voted || voteM.isPending}
                                onClick={() => voteM.mutate(opt.id)}
                                className="relative z-10 w-full text-left text-[hsl(var(--foreground))]"
                            >
                                <span className="font-medium">
                                    {opt.optionText}
                                </span>
                                {totalVotes > 0 && (
                                    <span className="float-right text-sm text-[hsl(var(--muted-foreground))]">
                                        {percentage}%
                                    </span>
                                )}
                            </button>
                        </div>
                    );
                })}
            </div>

            {poll.voted && (
                <p className="mt-3 text-sm text-green-400 font-semibold">
                    You’ve voted ✅ (voting locked)
                </p>
            )}
        </div>
    );
}
