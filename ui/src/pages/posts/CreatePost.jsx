import { useState } from "react"
import Button from "@/components/ui/Button"
import Input from "@/components/ui/Input"
import Textarea from "@/components/ui/Textarea"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import { useNavigate } from "react-router-dom"

const ANIMALS = ["DOG", "CAT", "BIRD", "FISH", "PIG", "OTHER"]
const ANIMAL_EMOJIS = {
    DOG: "🐕",
    CAT: "🐈",
    BIRD: "🐦",
    FISH: "🐠",
    PIG: "🐷",
    OTHER: "🐾",
}

export default function CreatePost() {
    const [title, setTitle] = useState("")
    const [content, setContent] = useState("")
    const [animalType, setAnimalType] = useState("OTHER")
    const [file, setFile] = useState(null)
    const [isUploading, setIsUploading] = useState(false)
    const nav = useNavigate()

    const submit = async (e) => {
        e.preventDefault()
        if (!file) return toast.error("📸 Image is required")
        setIsUploading(true)

        const data = new FormData()
        data.append("data", JSON.stringify({ title, content, animalType }))
        data.append("file", file)

        try {
            const res = await api.post("/api/posts", data, {
                headers: { "Content-Type": "multipart/form-data" },
            })
            toast.success("🎉 Post created!")
            nav(`/posts/${res.data.id}`)
        } catch (e) {
            toast.error(e.message || "🚫 Failed to create post")
        } finally {
            setIsUploading(false)
        }
    }

    return (
        <div className="relative min-h-screen py-8 px-4">
            <div className="max-w-xl mx-auto">
                {/* Glass container */}
                <div
                    className="rounded-3xl p-8 border border-[hsl(var(--border))]
              bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
              backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
              transition-all duration-300"
                >
                    {/* Header */}
                    <div className="text-center mb-8">
                        <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                            Create New Post
                        </h1>
                        <p className="text-[hsl(var(--muted-foreground))] mt-2">
                            Share your pet moments with the community
                        </p>
                    </div>

                    <form onSubmit={submit} className="space-y-6">
                        {/* Title */}
                        <div className="space-y-2">
                            <label className="text-sm font-semibold text-[hsl(var(--foreground))]">
                                Title *
                            </label>
                            <Input
                                placeholder="Enter post title..."
                                value={title}
                                onChange={(e) => setTitle(e.target.value)}
                                required
                                className="w-full"
                            />
                        </div>

                        {/* Content */}
                        <div className="space-y-2">
                            <label className="text-sm font-semibold text-[hsl(var(--foreground))]">
                                Content (optional)
                            </label>
                            <Textarea
                                placeholder="Tell us about your pet..."
                                value={content}
                                onChange={(e) => setContent(e.target.value)}
                                className="w-full min-h-32"
                            />
                        </div>

                        {/* Animal Type */}
                        <div className="space-y-2">
                            <label className="text-sm font-semibold text-[hsl(var(--foreground))]">
                                Pet Type
                            </label>
                            <div className="relative">
                                <select
                                    value={animalType}
                                    onChange={(e) => setAnimalType(e.target.value)}
                                    className="w-full rounded-xl px-4 py-3 bg-[hsl(var(--card))]
                      border border-[hsl(var(--border))]
                      text-[hsl(var(--foreground))] appearance-none
                      focus:ring-2 focus:ring-[hsl(var(--ring))]
                      focus:border-[hsl(var(--ring))] transition-all duration-300"
                                >
                                    {ANIMALS.map((a) => (
                                        <option
                                            key={a}
                                            value={a}
                                            className="bg-[hsl(var(--background))] text-[hsl(var(--foreground))]"
                                        >
                                            {ANIMAL_EMOJIS[a]} {a}
                                        </option>
                                    ))}
                                </select>
                                <div className="absolute right-3 top-1/2 transform -translate-y-1/2 text-[hsl(var(--muted-foreground))]">
                                    ▼
                                </div>
                            </div>
                        </div>

                        {/* File Upload */}
                        <div className="space-y-2">
                            <label className="text-sm font-semibold text-[hsl(var(--foreground))]">
                                Pet Photo *
                            </label>
                            <div
                                className="border-2 border-dashed border-[hsl(var(--border))]
                  rounded-2xl p-6 text-center transition-all duration-300
                  hover:border-[hsl(var(--ring))] hover:bg-[hsl(var(--muted))]/10"
                            >
                                <input
                                    type="file"
                                    accept="image/*"
                                    onChange={(e) => setFile(e.target.files?.[0])}
                                    className="hidden"
                                    id="file-upload"
                                    required
                                />
                                <label htmlFor="file-upload" className="cursor-pointer">
                                    <div className="text-4xl mb-2">📸</div>
                                    <p className="text-[hsl(var(--muted-foreground))]">
                                        {file ? file.name : "Click to upload or drag and drop"}
                                    </p>
                                    <p className="text-xs text-[hsl(var(--muted-foreground))] mt-1">
                                        PNG, JPG, GIF up to 10MB
                                    </p>
                                </label>
                            </div>

                            {file && (
                                <div className="text-sm text-green-500 flex items-center gap-1">
                                    <span>✅</span> File selected: {file.name}
                                </div>
                            )}
                        </div>

                        {/* Submit */}
                        <Button
                            type="submit"
                            disabled={isUploading || !file}
                            className="w-full py-3 text-lg font-semibold mt-6 bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_25px_hsl(var(--ring))]"
                        >
                            {isUploading ? (
                                <span className="flex items-center gap-2">
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  Publishing...
                </span>
                            ) : (
                                <span className="flex items-center gap-2">
                  🚀 Publish Post
                </span>
                            )}
                        </Button>
                    </form>
                </div>

                {/* Background */}
                <div className="absolute inset-0 -z-10 overflow-hidden">
                    <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl animate-float"></div>
                    <div
                        className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl animate-float"
                        style={{ animationDelay: "2s" }}
                    ></div>
                </div>
            </div>
        </div>
    )
}
