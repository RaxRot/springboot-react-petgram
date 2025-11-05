import { useParams, Link } from "react-router-dom"
import { useQuery } from "@tanstack/react-query"
import { api } from "@/lib/axios.js"
import Skeleton from "@/components/ui/Skeleton.jsx"
import Button from "@/components/ui/Button.jsx"
import RandomPetGame from "@/components/RandomPetGame.jsx"
import { motion } from "framer-motion"
import { toast } from "sonner"
import axios from "axios"

export default function PetDetails() {
    const { id } = useParams()

    const petQ = useQuery({
        queryKey: ["pet", id],
        queryFn: async () => (await api.get(`/api/public/pets/${id}`)).data,
    })

    if (petQ.isLoading) return <Skeleton className="w-full h-96 rounded-2xl" />

    if (petQ.isError)
        return (
            <div className="text-center text-red-500 mt-20">
                <p>⚠️ Pet not found</p>
                <Link
                    to="/"
                    className="text-cyan-500 underline mt-2 inline-block hover:text-cyan-400 transition"
                >
                    Go Home
                </Link>
            </div>
        )

    const pet = petQ.data

    const analyzePet = async () => {
        if (!pet.photoUrl) {
            toast.error("No photo available for analysis 🐾")
            return
        }

        try {
            const { data } = await axios.post("http://localhost:5000/analyze", {
                imageUrl: pet.photoUrl,
            })

            toast.custom(() => (
                <div className="rounded-2xl border border-border bg-card/90 p-4 text-left shadow-lg max-w-xs">
                    <div className="text-lg font-bold mb-1">AI Analysis ✅</div>
                    <div className="text-sm text-muted-foreground space-y-1">
                        <p>🧬 Species: <b>{data.species}</b></p>
                        {data.breed && (
                            <p>🏷️ Breed: <b>{data.breed}</b></p>
                        )}
                        <p>📝 {data.description}</p>
                    </div>
                </div>
            ), { duration: 6000 })

        } catch (e) {
            toast.error("AI analysis failed 😿")
        }
    }

    return (
        <div
            className="max-w-3xl mx-auto p-6 space-y-10
      bg-[hsl(var(--card))]/80 backdrop-blur-xl
      border border-[hsl(var(--border))]
      text-[hsl(var(--foreground))]
      rounded-3xl shadow-[0_0_30px_rgba(56,189,248,0.15)]
      transition-all duration-300"
        >
            {/* 🐾 Фото питомца */}
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.6, ease: "easeOut" }}
                className="relative"
            >
                <img
                    src={pet.photoUrl || "https://placehold.co/600x400?text=No+Photo"}
                    alt={pet.name}
                    className="w-full h-96 object-cover rounded-2xl border border-[hsl(var(--border))]"
                />
            </motion.div>

            {/* ℹ️ Информация */}
            <div className="space-y-3 text-center">
                <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                    {pet.name}
                </h1>
                <p className="text-[hsl(var(--muted-foreground))] text-lg">
                    <span className="capitalize">{pet.type.toLowerCase()}</span>
                    {pet.age && ` • ${pet.age} years old`}
                    {pet.breed && ` • ${pet.breed}`}
                </p>

                {pet.description && (
                    <p className="text-[hsl(var(--foreground))]/80 leading-relaxed max-w-xl mx-auto">
                        {pet.description}
                    </p>
                )}
            </div>

            {/* 👤 Владелец + Analyze Button */}
            <div className="pt-4 border-t border-[hsl(var(--border))] flex items-center justify-between">
                <div>
                    <p className="text-sm text-[hsl(var(--muted-foreground))]">Owner:</p>
                    <Link
                        to={`/u/${pet.ownerUsername}`}
                        className="font-semibold text-cyan-600 dark:text-cyan-400 hover:text-cyan-300 transition"
                    >
                        @{pet.ownerUsername}
                    </Link>
                </div>

                <div className="flex gap-2">
                    <Button
                        onClick={analyzePet}
                        className="bg-gradient-to-r from-purple-500 to-pink-500 hover:shadow-[0_0_25px_rgba(236,72,153,0.5)]"
                    >
                        🔍 Analyze Pet
                    </Button>

                    <Button
                        onClick={() => window.history.back()}
                        className="bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_25px_rgba(56,189,248,0.4)]"
                    >
                        ← Back
                    </Button>
                </div>
            </div>

            {/* 🧸 Мини-игра: случайный питомец */}
            <RandomPetGame />
        </div>
    )
}
