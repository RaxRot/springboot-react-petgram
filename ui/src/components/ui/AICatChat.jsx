import { useState } from "react"
import axios from "axios"
import Button from "@/components/ui/Button"

//  fetches an animal fact on button click
export default function AICatChat() {
    const [open, setOpen] = useState(false)
    const [messages, setMessages] = useState([
        { from: "bot", text: "Tap “Get fact” and I’ll tell you a fun animal fact! 🐾" },
    ])
    const [loading, setLoading] = useState(false)

    const getFact = async () => {
        setLoading(true)
        try {
            // Backend /chat can return {reply: "..."} or {fact: "..."}
            const res = await axios.post("http://localhost:5000/chat", {})
            const text = res.data?.reply ?? res.data?.fact ?? "meow?"
            setMessages((prev) => [...prev, { from: "bot", text }])
        } catch (err) {
            setMessages((prev) => [
                ...prev,
                { from: "bot", text: "Meow... I couldn’t fetch a fact right now 😿" },
            ])
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="fixed bottom-6 right-6 z-50">

            {!open && (
                <button
                    onClick={() => setOpen(true)}
                    className="w-14 h-14 bg-gradient-to-r from-cyan-400 to-purple-600 text-3xl rounded-full shadow-[0_0_25px_rgba(56,189,248,0.4)] hover:scale-110 transition-all"
                    aria-label="Open PetCat chat"
                >
                    🐱
                </button>
            )}


            {open && (
                <div className="w-80 h-96 bg-[hsl(var(--card))]/95 backdrop-blur-xl border border-[hsl(var(--border))] rounded-2xl shadow-[0_0_30px_rgba(56,189,248,0.2)] flex flex-col overflow-hidden transition-all">
                    {/* 🔹 Top bar */}
                    <div className="flex justify-between items-center px-4 py-3 bg-gradient-to-r from-cyan-400 to-purple-600 text-white font-semibold">
                        <span>🐾 PetCat — Animal Facts</span>
                        <button
                            onClick={() => setOpen(false)}
                            className="hover:scale-110 transition-transform"
                            aria-label="Close"
                        >
                            ✖️
                        </button>
                    </div>

                    <div className="flex-1 overflow-y-auto p-3 space-y-2 scrollbar-thin scrollbar-thumb-cyan-400/50">
                        {messages.map((msg, i) => (
                            <div
                                key={i}
                                className={`max-w-[85%] px-3 py-2 rounded-xl text-sm ${
                                    msg.from === "user"
                                        ? "ml-auto bg-gradient-to-r from-cyan-400 to-purple-600 text-white shadow-[0_0_10px_rgba(56,189,248,0.4)]"
                                        : "bg-[hsl(var(--muted))]/20 text-[hsl(var(--foreground))]"
                                }`}
                            >
                                {msg.text}
                            </div>
                        ))}
                        {loading && (
                            <div className="text-[hsl(var(--muted-foreground))] text-sm">
                                Cat is fetching a fact... 💤
                            </div>
                        )}
                    </div>


                    <div className="p-3 border-t border-[hsl(var(--border))] flex gap-2 justify-center">
                        <Button onClick={getFact} size="sm" variant="primary" disabled={loading}>
                            {loading ? "Fetching..." : "Get fact"}
                        </Button>
                    </div>
                </div>
            )}
        </div>
    )
}