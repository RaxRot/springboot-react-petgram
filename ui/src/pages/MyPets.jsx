import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { motion, useReducedMotion } from "framer-motion"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import Button from "@/components/ui/Button"
import Input from "@/components/ui/Input"
import Textarea from "@/components/ui/Textarea"
import Skeleton from "@/components/ui/Skeleton"
import { Link } from "react-router-dom"

export default function MyPets() {
    const qc = useQueryClient()
    const prefersReducedMotion = useReducedMotion()
    const [showModal, setShowModal] = useState(false)
    const [confirmPet, setConfirmPet] = useState(null)

    const [form, setForm] = useState({
        name: "",
        type: "OTHER",
        breed: "",
        age: "",
        description: "",
    })
    const [file, setFile] = useState(null)

    // --- GET MY PETS ---
    const petsQ = useQuery({
        queryKey: ["myPets"],
        queryFn: async () => (await api.get("/api/user/pets")).data,
    })

    // --- ADD PET ---
    const addPet = useMutation({
        mutationFn: async () => {
            if (!form.name.trim()) throw new Error("Pet name is required")

            const fd = new FormData()
            fd.append("data", JSON.stringify(form))
            if (file) fd.append("file", file)

            const { data } = await api.post("/api/pets", fd, {
                headers: { "Content-Type": "multipart/form-data" },
            })
            return data
        },
        onSuccess: () => {
            toast.success("🐾 Pet added!")
            qc.invalidateQueries({ queryKey: ["myPets"] })
            setShowModal(false)
            setForm({ name: "", type: "OTHER", breed: "", age: "", description: "" })
            setFile(null)
        },
        onError: (e) => toast.error(e?.response?.data?.message || "Failed to add pet"),
    })

    // --- DELETE PET ---
    const delPet = useMutation({
        mutationFn: async (id) => api.delete(`/api/pets/${id}`),
        onSuccess: () => {
            toast.info("🗑️ Pet deleted")
            qc.invalidateQueries({ queryKey: ["myPets"] })
        },
        onError: () => toast.error("Failed to delete pet"),
    })

    // --- Motion Variants ---
    const gridVariants = {
        hidden: { opacity: 0 },
        visible: {
            opacity: 1,
            transition: { when: "beforeChildren", staggerChildren: 0.08 },
        },
    }

    const cardVariants = {
        hidden: { opacity: 0, y: 20, scale: 0.97 },
        visible: {
            opacity: 1,
            y: 0,
            scale: 1,
            transition: { duration: 0.3, ease: "easeOut" },
        },
    }

    return (
        <div className="max-w-5xl mx-auto p-6 space-y-8">
            {/* Header */}
            <div className="flex items-center justify-between">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    My Pets 🐾
                </h1>
                <Button
                    onClick={() => setShowModal(true)}
                    className="bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_20px_hsl(var(--ring))]"
                >
                    ➕ Add Pet
                </Button>
            </div>

            {/* Loading */}
            {petsQ.isLoading && (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {[...Array(6)].map((_, i) => (
                        <div
                            key={i}
                            className="bg-card/70 rounded-2xl p-4 border border-border"
                        >
                            <Skeleton className="h-48 w-full rounded-xl mb-3" variant="image" />
                            <Skeleton className="h-4 w-32 mb-2" variant="text" />
                            <Skeleton className="h-3 w-24" variant="text" />
                        </div>
                    ))}
                </div>
            )}

            {/* Empty */}
            {!petsQ.isLoading && petsQ.data?.content?.length === 0 && (
                <div className="text-center py-16 bg-card/70 border border-border rounded-2xl shadow-[0_0_25px_rgba(56,189,248,0.08)] backdrop-blur-xl">
                    <div className="text-6xl mb-4">😺</div>
                    <p className="text-foreground text-lg">No pets yet</p>
                    <p className="text-muted-foreground text-sm">
                        Add your first pet using the button above
                    </p>
                </div>
            )}

            {/* Pets grid */}
            <motion.div
                className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3"
                variants={gridVariants}
                initial="hidden"
                animate="visible"
            >
                {petsQ.data?.content?.map((pet) => (
                    <motion.article
                        key={pet.id}
                        variants={cardVariants}
                        whileHover={prefersReducedMotion ? {} : { scale: 1.04, rotate: 0.4 }}
                        whileTap={prefersReducedMotion ? {} : { scale: 0.98 }}
                        className="relative group bg-card/80 rounded-2xl p-4 border border-border
                        hover:shadow-[0_0_35px_hsl(var(--ring))] transition-all duration-300 backdrop-blur-xl
                        before:absolute before:inset-0 before:rounded-2xl before:p-[1px] before:bg-gradient-to-br
                        before:from-cyan-400/20 before:to-purple-600/20 before:opacity-0
                        group-hover:before:opacity-100 before:pointer-events-none"
                    >
                        <Link to={`/pets/${pet.id}`}>
                            {pet.photoUrl && (
                                <motion.div
                                    className="rounded-xl overflow-hidden mb-3"
                                    whileHover={{ scale: 1.02 }}
                                    transition={{ duration: 0.25 }}
                                >
                                    <img
                                        src={pet.photoUrl}
                                        alt={pet.name}
                                        className="w-full h-48 object-cover"
                                    />
                                </motion.div>
                            )}
                        </Link>

                        <h3 className="text-xl font-semibold text-foreground">
                            {pet.name}
                        </h3>
                        <p className="text-sm text-muted-foreground capitalize">
                            {pet.type.toLowerCase()} • {pet.age || "?"} years
                        </p>
                        {pet.breed && (
                            <p className="text-sm text-muted-foreground italic">
                                {pet.breed}
                            </p>
                        )}

                        {/* Delete button */}
                        <button
                            onClick={(e) => {
                                e.stopPropagation()
                                e.preventDefault()
                                setConfirmPet(pet)
                            }}
                            className="absolute top-3 right-3 text-muted-foreground hover:text-destructive bg-background/50 backdrop-blur-md
                            p-2 rounded-full border border-border z-20 transition-all duration-200 hover:scale-110 active:scale-95"
                            title="Delete pet"
                        >
                            🗑️
                        </button>

                        <Link
                            to={`/pets/${pet.id}`}
                            className="mt-3 inline-block text-sm font-medium text-primary hover:underline"
                        >
                            View details →
                        </Link>
                    </motion.article>
                ))}
            </motion.div>

            {/* Add Pet Modal */}
            {showModal && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
                    <div className="bg-card/90 border border-border text-foreground
                    p-6 rounded-2xl w-full max-w-md space-y-4 shadow-[0_0_30px_hsl(var(--ring))]">
                        <h2 className="text-2xl font-bold text-center mb-4 bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                            Add a Pet 🐕
                        </h2>

                        <Input
                            placeholder="Name"
                            value={form.name}
                            onChange={(e) => setForm({ ...form, name: e.target.value })}
                        />

                        <select
                            className="w-full p-2 rounded-xl border border-border bg-background
                            text-foreground focus:outline-none focus:ring-2 focus:ring-primary transition-all"
                            value={form.type}
                            onChange={(e) => setForm({ ...form, type: e.target.value })}
                        >
                            <option value="DOG" className="bg-background text-foreground">Dog</option>
                            <option value="CAT" className="bg-background text-foreground">Cat</option>
                            <option value="PARROT" className="bg-background text-foreground">Parrot</option>
                            <option value="RABBIT" className="bg-background text-foreground">Rabbit</option>
                            <option value="HAMSTER" className="bg-background text-foreground">Hamster</option>
                            <option value="OTHER" className="bg-background text-foreground">Other</option>
                        </select>

                        <Input
                            placeholder="Breed"
                            value={form.breed}
                            onChange={(e) => setForm({ ...form, breed: e.target.value })}
                        />

                        <Input
                            type="number"
                            placeholder="Age"
                            value={form.age}
                            onChange={(e) => setForm({ ...form, age: e.target.value })}
                        />

                        <Textarea
                            placeholder="Description"
                            rows={3}
                            value={form.description}
                            onChange={(e) => setForm({ ...form, description: e.target.value })}
                        />

                        <input
                            type="file"
                            accept="image/*"
                            className="w-full text-sm text-muted-foreground file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-primary/10 file:text-primary hover:file:bg-primary/20 transition"
                            onChange={(e) => setFile(e.target.files?.[0])}
                        />

                        <div className="flex justify-end gap-2 mt-4">
                            <Button variant="outline" onClick={() => setShowModal(false)}>
                                Cancel
                            </Button>
                            <Button
                                onClick={() => addPet.mutate()}
                                disabled={addPet.isPending}
                                className="bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_25px_hsl(var(--ring))]"
                            >
                                {addPet.isPending ? "Saving..." : "Save"}
                            </Button>
                        </div>
                    </div>
                </div>
            )}

            {/* Delete Confirmation Modal */}
            {confirmPet && (
                <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50">
                    <div className="bg-card/90 text-foreground p-6 rounded-2xl w-full max-w-sm
                    border border-border shadow-[0_0_25px_hsl(var(--ring))] text-center space-y-4">
                        <h2 className="text-xl font-semibold">
                            Delete <span className="text-destructive">{confirmPet.name}</span>?
                        </h2>
                        <p className="text-sm text-muted-foreground">
                            This action cannot be undone.
                        </p>

                        <div className="flex justify-center gap-3 pt-4">
                            <Button variant="outline" onClick={() => setConfirmPet(null)}>
                                Cancel
                            </Button>
                            <Button
                                onClick={() => {
                                    delPet.mutate(confirmPet.id)
                                    setConfirmPet(null)
                                }}
                                className="bg-gradient-to-r from-red-500 to-pink-600 hover:shadow-[0_0_25px_rgba(239,68,68,0.5)]"
                            >
                                Yes, delete
                            </Button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    )
}
