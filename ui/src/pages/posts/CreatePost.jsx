import { useState } from "react"
import Button from "@/components/ui/Button"
import Input from "@/components/ui/Input"
import Textarea from "@/components/ui/Textarea"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import { useNavigate } from "react-router-dom"

const ANIMALS = ["DOG","CAT","BIRD","FISH","PIG","OTHER"]

export default function CreatePost() {
    const [title, setTitle] = useState("")
    const [content, setContent] = useState("")
    const [animalType, setAnimalType] = useState("OTHER")
    const [file, setFile] = useState(null)
    const nav = useNavigate()

    const submit = async (e) => {
        e.preventDefault()
        if (!file) return toast.error("Image is required")
        const data = new FormData()
        data.append("data", JSON.stringify({ title, content, animalType }))
        data.append("file", file)
        try {
            const res = await api.post("/api/posts", data, { headers: { "Content-Type": "multipart/form-data" } })
            toast.success("Post created")
            nav(`/posts/${res.data.id}`)
        } catch (e) {
            toast.error(e.message)
        }
    }

    return (
        <form onSubmit={submit} className="max-w-xl mx-auto space-y-3">
            <h1 className="text-2xl font-bold">Create Post</h1>
            <Input placeholder="Title" value={title} onChange={e=>setTitle(e.target.value)} />
            <Textarea placeholder="Content (optional)" value={content} onChange={e=>setContent(e.target.value)} />
            <select value={animalType} onChange={e=>setAnimalType(e.target.value)} className="w-full rounded-xl border px-3 py-2">
                {ANIMALS.map(a => <option key={a} value={a}>{a}</option>)}
            </select>
            <input type="file" accept="image/*" onChange={e=>setFile(e.target.files?.[0])} />
            <Button type="submit" className="w-full">Publish</Button>
        </form>
    )
}
