import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
import Button from "@/components/ui/Button"

const PETS = [
    { emoji: "🐶", name: "Doggo" },
    { emoji: "🐱", name: "Mittens" },
    { emoji: "🐰", name: "Bunny" },
    { emoji: "🐹", name: "Hammy" },
    { emoji: "🐦", name: "Chirpy" },
]

// 🎨 Фоны под настроение
const MOOD_COLORS = {
    hungry: "linear-gradient(135deg, #1e3a8a, #2563eb, #0ea5e9)",
    normal: "linear-gradient(135deg, #0891b2, #06b6d4, #67e8f9)",
    happy: "linear-gradient(135deg, #f472b6, #fb923c, #facc15)",
    sleepy: "linear-gradient(135deg, #312e81, #6d28d9, #9333ea)",
    night: "linear-gradient(135deg, #0f172a, #1e293b, #475569)",
}

// 💭 случайные фразы
const THOUGHTS = {
    hungry: ["Feed me!", "I'm starving!", "So hungry 😿"],
    normal: ["Feeling good!", "Hello there!", "Let’s play! 🐾"],
    happy: ["Yummy!", "Love you 💕", "Best day ever! 😋"],
    sleepy: ["Zzz... 😴", "So full...", "Good night 🌙"],
}

export default function RandomPetGame() {
    const [pet, setPet] = useState(null)
    const [fullness, setFullness] = useState(0)
    const [isEating, setIsEating] = useState(false)
    const [mood, setMood] = useState("hungry")
    const [thought, setThought] = useState("")
    const [isNight, setIsNight] = useState(false)

    // 🐾 выбираем случайного питомца раз в день
    useEffect(() => {
        const today = new Date().toISOString().split("T")[0]
        const savedDate = localStorage.getItem("randomPetDate")
        const savedPet = localStorage.getItem("randomPetName")

        if (savedPet && savedDate === today) {
            const found = PETS.find((p) => p.name === savedPet)
            setPet(found)
        } else {
            const randomPet = PETS[Math.floor(Math.random() * PETS.length)]
            setPet(randomPet)
            localStorage.setItem("randomPetName", randomPet.name)
            localStorage.setItem("randomPetDate", today)
        }
    }, [])

    // ⏰ определяем ночь (22:00–6:00)
    useEffect(() => {
        const hour = new Date().getHours()
        setIsNight(hour >= 22 || hour < 6)
    }, [])

    // 💾 загружаем и сохраняем сытость
    useEffect(() => {
        if (!pet) return
        const saved = localStorage.getItem(`randomPetFullness_${pet.name}`)
        if (saved) setFullness(Number(saved))
    }, [pet])

    useEffect(() => {
        if (pet) localStorage.setItem(`randomPetFullness_${pet.name}`, fullness.toString())
        if (fullness < 20) setMood("hungry")
        else if (fullness < 60) setMood("normal")
        else if (fullness < 90) setMood("happy")
        else setMood("sleepy")
    }, [fullness, pet])

    // 🍖 кормим
    const feedPet = () => {
        if (isEating || mood === "sleepy") return
        setIsEating(true)
        setFullness((prev) => Math.min(prev + 15, 100))

        const phrase = THOUGHTS["happy"][Math.floor(Math.random() * THOUGHTS.happy.length)]
        setThought(phrase)

        setTimeout(() => {
            setIsEating(false)
            setThought("")
        }, 2000)
    }

    if (!pet) return null

    const moodEmoji =
        mood === "hungry"
            ? "😿"
            : mood === "normal"
                ? "😺"
                : mood === "happy"
                    ? "😋"
                    : "😴"

    const bg =
        isNight && mood !== "happy"
            ? MOOD_COLORS.night
            : MOOD_COLORS[mood] || MOOD_COLORS.normal

    return (
        <motion.div
            className="mt-10 p-6 rounded-3xl border border-cyan-400/20 relative overflow-hidden shadow-[0_0_35px_rgba(56,189,248,0.2)]"
            style={{
                background: bg,
                color: "#fff",
            }}
            animate={{ background: bg }}
            transition={{ duration: 1.5, ease: "easeInOut" }}
        >
            {/* ✨ Звёзды ночью */}
            {isNight && (
                <div className="absolute inset-0 overflow-hidden">
                    {[...Array(20)].map((_, i) => (
                        <motion.div
                            key={i}
                            className="absolute bg-white rounded-full"
                            style={{
                                width: 2,
                                height: 2,
                                top: `${Math.random() * 100}%`,
                                left: `${Math.random() * 100}%`,
                            }}
                            animate={{ opacity: [0.3, 1, 0.3] }}
                            transition={{ duration: 2 + Math.random() * 2, repeat: Infinity }}
                        />
                    ))}
                </div>
            )}

            <h2 className="text-3xl font-bold mb-4 drop-shadow-lg">
                Random Pet Game 🧸
            </h2>

            {/* 🐾 питомец */}
            <motion.div
                animate={
                    isEating
                        ? { scale: [1, 1.25, 1], rotate: [0, -10, 10, 0] }
                        : mood === "sleepy"
                            ? { y: [0, 3, 0], scale: [1, 0.98, 1] }
                            : { y: [0, -3, 0] }
                }
                transition={{ duration: 2, repeat: Infinity, ease: "easeInOut" }}
                className="text-8xl select-none z-10 relative"
            >
                {pet.emoji}
            </motion.div>

            {/* 💬 мысли */}
            <AnimatePresence mode="wait">
                {thought && (
                    <motion.div
                        key={thought}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: -10 }}
                        exit={{ opacity: 0, y: -30 }}
                        transition={{ duration: 0.5 }}
                        className="absolute left-1/2 -translate-x-1/2 top-16 bg-white/20 backdrop-blur-md text-white px-4 py-2 rounded-2xl shadow-lg text-sm"
                    >
                        {thought}
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 💬 основное настроение */}
            <p className="mt-2 text-lg font-semibold drop-shadow-sm">
                {moodEmoji}{" "}
                {mood === "sleepy"
                    ? "Zzz..."
                    : mood === "happy"
                        ? "Feeling great!"
                        : mood === "hungry"
                            ? "Feed me!"
                            : "All good!"}
            </p>

            {/* шкала сытости */}
            <div className="relative h-4 w-full bg-white/20 rounded-full overflow-hidden mt-4">
                <motion.div
                    className="absolute top-0 left-0 h-full bg-gradient-to-r from-green-400 to-cyan-300"
                    animate={{ width: `${fullness}%` }}
                    transition={{ duration: 0.6 }}
                />
            </div>
            <p className="text-sm opacity-80 mt-1">{fullness}% full</p>

            {/* кнопка */}
            <Button
                onClick={feedPet}
                disabled={isEating || mood === "sleepy"}
                className="mt-5 bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_25px_rgba(56,189,248,0.6)]"
            >
                {isNight
                    ? "🌙 Sleeping..."
                    : isEating
                        ? "🍖 Eating..."
                        : fullness >= 100
                            ? "💤 Full"
                            : "Feed me!"}
            </Button>
        </motion.div>
    )
}
