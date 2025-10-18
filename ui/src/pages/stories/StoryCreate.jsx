import { useState } from "react"
import { useNavigate } from "react-router-dom"
import { api } from "@/lib/axios"
import Button from "@/components/ui/Button"
import { toast } from "sonner"

export default function StoryCreate() {
    const [file, setFile] = useState(null)
    const [preview, setPreview] = useState(null)
    const [uploading, setUploading] = useState(false)
    const nav = useNavigate()

    const onFileChange = (e) => {
        const f = e.target.files?.[0]
        if (!f) return
        if (!f.type.startsWith("image/")) {
            toast.error("Only image files are allowed")
            return
        }
        setFile(f)
        setPreview(URL.createObjectURL(f))
    }

    const handleSubmit = async () => {
        if (!file) {
            toast.error("Please select an image")
            return
        }

        try {
            setUploading(true)
            const fd = new FormData()
            fd.append("file", file)
            await api.post("/api/stories", fd, {
                headers: { "Content-Type": "multipart/form-data" },
            })
            toast.success("Story uploaded 🎉")
            nav("/")
        } catch (e) {
            toast.error(e.message || "Upload failed")
        } finally {
            setUploading(false)
        }
    }

    return (
        <div className="max-w-lg mx-auto rounded-2xl border border-border
                        bg-card/80 backdrop-blur-xl p-6 shadow-lg
                        space-y-6 transition-all duration-300">

            <h1 className="text-2xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                Create a Story 📸
            </h1>

            <input
                type="file"
                accept="image/*"
                onChange={onFileChange}
                className="block w-full text-sm text-foreground
                           file:mr-4 file:py-2 file:px-4 file:rounded-full
                           file:border-0 file:text-sm file:font-semibold
                           file:bg-primary/10 file:text-primary
                           hover:file:bg-primary/20 transition"
            />

            {preview && (
                <div className="mt-4">
                    <img
                        src={preview}
                        alt="preview"
                        className="rounded-xl border border-border shadow-md"
                    />
                </div>
            )}

            <Button
                onClick={handleSubmit}
                disabled={uploading}
                className="w-full py-2 bg-gradient-to-r from-cyan-400 to-purple-600
                           text-white font-semibold rounded-xl shadow-md hover:scale-105
                           transition-transform"
            >
                {uploading ? "Uploading..." : "Post Story"}
            </Button>
        </div>
    )
}