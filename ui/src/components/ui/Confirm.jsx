import { toast } from "sonner"
import { motion } from "framer-motion"

export function confirmToast({
                                 title,
                                 desc,
                                 okText = "Confirm",
                                 cancelText = "Cancel",
                                 duration = 7000,
                             }) {
    return new Promise((resolve) => {
        toast.custom(
            (t) => (
                <motion.div
                    initial={{ opacity: 0, scale: 0.9, y: 10 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.9, y: -10 }}
                    transition={{ duration: 0.25 }}
                    className="relative w-[340px] sm:w-[380px] p-6 rounded-2xl overflow-hidden border border-white/10 backdrop-blur-xl shadow-[0_0_25px_rgba(0,255,255,0.1)]"
                >
                    {/* ✨ Background glow layers */}
                    <div className="absolute inset-0 bg-gradient-to-br from-cyan-500/10 via-purple-600/10 to-transparent animate-pulse-slow" />
                    <div className="absolute inset-0 rounded-2xl bg-[radial-gradient(circle_at_top_left,#06b6d4_0%,transparent_60%)] opacity-20 blur-2xl" />

                    {/* 🔮 Main content */}
                    <div className="relative z-10 text-center">
                        <h3 className="text-xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent drop-shadow-[0_0_10px_rgba(0,255,255,0.2)]">
                            {title}
                        </h3>
                        {desc && (
                            <p className="mt-2 text-sm text-gray-300 leading-relaxed opacity-90">
                                {desc}
                            </p>
                        )}

                        {/* 💫 Buttons */}
                        <div className="flex justify-center gap-4 mt-5">
                            <motion.button
                                whileHover={{ scale: 1.07 }}
                                whileTap={{ scale: 0.95 }}
                                onClick={() => {
                                    toast.dismiss(t)
                                    resolve(true)
                                }}
                                className="px-5 py-2.5 rounded-xl text-sm font-semibold text-white border-0 shadow-lg
                  bg-gradient-to-r from-cyan-400 to-purple-600 hover:from-cyan-500 hover:to-purple-700
                  shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all duration-300"
                            >
                                {okText}
                            </motion.button>

                            <motion.button
                                whileHover={{ scale: 1.05 }}
                                whileTap={{ scale: 0.95 }}
                                onClick={() => {
                                    toast.dismiss(t)
                                    resolve(false)
                                }}
                                className="px-5 py-2.5 rounded-xl text-sm font-semibold text-gray-300 border border-white/20
                  hover:bg-white/10 hover:text-white backdrop-blur-sm transition-all duration-300"
                            >
                                {cancelText}
                            </motion.button>
                        </div>
                    </div>

                    {/* 🌈 Bottom glow line */}
                    <motion.div
                        initial={{ x: "-100%" }}
                        animate={{ x: "100%" }}
                        transition={{ duration: 2, repeat: Infinity }}
                        className="absolute bottom-0 left-0 h-[2px] w-full bg-gradient-to-r from-transparent via-cyan-400 to-transparent opacity-60"
                    />
                </motion.div>
            ),
            {
                duration,
                style: {
                    background: "transparent",
                    border: "none",
                    boxShadow: "none",
                    padding: 0,
                },
            }
        )
    })
}
