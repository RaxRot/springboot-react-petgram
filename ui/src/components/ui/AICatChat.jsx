import { useState } from "react"
import axios from "axios"
import Button from "@/components/ui/Button"

// 🐱 Floating AI Cat Chat component
export default function AICatChat() {
    // State variables
    const [open, setOpen] = useState(false)            // controls open/close of the chat window
    const [input, setInput] = useState("")             // current text in input field
    const [messages, setMessages] = useState([
        { from: "bot", text: "Meow! I’m Petgram’s chat cat, wanna talk? 🐾" },
    ]) // array of chat messages
    const [loading, setLoading] = useState(false)      // shows “cat is thinking” while waiting for response

    // 📨 Send message to local AI server (Flask backend)
    const sendMessage = async () => {
        if (!input.trim()) return                        // ignore empty messages

        // Add user message to the chat
        const userMsg = { from: "user", text: input }
        setMessages((prev) => [...prev, userMsg])
        setInput("")
        setLoading(true)

        try {
            // POST request to your Flask API
            const res = await axios.post("http://localhost:5000/chat", {
                message: input,
            })

            // Add the AI cat's reply to the chat
            setMessages((prev) => [
                ...prev,
                { from: "bot", text: res.data.reply || "meow?" },
            ])
        } catch (err) {
            // Error handling if the server is down or fails
            setMessages((prev) => [
                ...prev,
                { from: "bot", text: "Meow... the server fell asleep 😿" },
            ])
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="fixed bottom-6 right-6 z-50">
            {/* 🐾 Floating cat button — appears when chat is closed */}
            {!open && (
                <button
                    onClick={() => setOpen(true)}
                    className="w-14 h-14 bg-gradient-to-r from-cyan-400 to-purple-600 text-3xl rounded-full shadow-[0_0_25px_rgba(56,189,248,0.4)] hover:scale-110 transition-all"
                >
                    🐱
                </button>
            )}

            {/* 💬 Chat window — visible when open */}
            {open && (
                <div className="w-80 h-96 bg-[hsl(var(--card))]/95 backdrop-blur-xl border border-[hsl(var(--border))] rounded-2xl shadow-[0_0_30px_rgba(56,189,248,0.2)] flex flex-col overflow-hidden transition-all">
                    {/* 🔹 Top bar */}
                    <div className="flex justify-between items-center px-4 py-3 bg-gradient-to-r from-cyan-400 to-purple-600 text-white font-semibold">
                        <span>🐾 Chat with PetCat</span>
                        <button
                            onClick={() => setOpen(false)}
                            className="hover:scale-110 transition-transform"
                        >
                            ✖
                        </button>
                    </div>

                    {/* 💭 Message area */}
                    <div className="flex-1 overflow-y-auto p-3 space-y-2 scrollbar-thin scrollbar-thumb-cyan-400/50">
                        {messages.map((msg, i) => (
                            <div
                                key={i}
                                className={`max-w-[80%] px-3 py-2 rounded-xl text-sm ${
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
                                Cat is thinking... 💤
                            </div>
                        )}
                    </div>

                    {/* ✏️ Input field + Send button */}
                    <div className="p-3 border-t border-[hsl(var(--border))] flex gap-2">
                        <input
                            value={input}
                            onChange={(e) => setInput(e.target.value)}
                            onKeyDown={(e) => e.key === "Enter" && sendMessage()} // allows pressing Enter
                            placeholder="Write to the cat..."
                            className="flex-1 px-3 py-2 rounded-xl bg-[hsl(var(--muted))]/15 border border-[hsl(var(--border))] text-sm focus:outline-none focus:ring-2 focus:ring-cyan-400/60"
                        />
                        <Button
                            onClick={sendMessage}
                            size="sm"
                            variant="primary"
                            disabled={loading}
                        >
                            Send
                        </Button>
                    </div>
                </div>
            )}
        </div>
    )
}
