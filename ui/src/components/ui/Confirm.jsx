import { toast } from "sonner"

export function confirmToast({ title, desc, okText = "Confirm", cancelText = "Cancel", duration = 5000 }) {
    return new Promise((resolve) => {
        const id = toast(
            (t) => (
                <div className="relative p-6 space-y-4 bg-gradient-to-br from-gray-900 to-black border border-cyan-400/20 rounded-2xl shadow-2xl shadow-cyan-500/20 backdrop-blur-xl">
                    {/* Glow effect */}
                    <div className="absolute inset-0 bg-cyan-500/5 rounded-2xl animate-pulse-slow"></div>

                    <div className="relative z-10 space-y-3">
                        <div className="font-bold text-lg text-white">{title}</div>
                        {desc && <div className="text-sm text-gray-300 opacity-90">{desc}</div>}

                        <div className="flex gap-3 pt-2">
                            <button
                                onClick={() => { toast.dismiss(t); resolve(true) }}
                                className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-cyan-400 to-purple-600 text-white font-semibold text-sm shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 hover:from-cyan-500 hover:to-purple-700 transition-all duration-300 hover:scale-105 active:scale-95 border-0"
                            >
                                {okText}
                            </button>
                            <button
                                onClick={() => { toast.dismiss(t); resolve(false) }}
                                className="px-5 py-2.5 rounded-xl bg-transparent border border-gray-600 text-gray-300 font-semibold text-sm hover:bg-white/5 hover:border-gray-500 hover:text-white transition-all duration-300 hover:scale-105 active:scale-95 backdrop-blur-sm"
                            >
                                {cancelText}
                            </button>
                        </div>
                    </div>
                </div>
            ),
            {
                duration,
                style: {
                    background: 'transparent',
                    border: 'none',
                    padding: 0,
                    boxShadow: 'none'
                }
            }
        )
    })
}