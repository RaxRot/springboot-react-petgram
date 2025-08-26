export default function PaymentSuccess() {
    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="max-w-md mx-auto text-center space-y-6">
                {/* Animated icon with confetti effect */}
                <div className="text-6xl animate-bounce">🎉</div>

                <h1 className="text-3xl font-bold bg-gradient-to-r from-green-400 to-cyan-600 bg-clip-text text-transparent">
                    Payment Successful!
                </h1>

                <p className="text-gray-600 dark:text-gray-400 text-lg">
                    Thank you for supporting PetSocial! Your contribution means a lot! 🐾
                </p>

                {/* Celebration confetti elements */}
                <div className="flex justify-center gap-2 py-2">
                    {['✨', '⭐', '💫', '🔥', '🐾'].map((emoji, i) => (
                        <span
                            key={i}
                            className="text-2xl animate-pulse"
                            style={{ animationDelay: `${i * 0.2}s` }}
                        >
                            {emoji}
                        </span>
                    ))}
                </div>

                {/* Action buttons */}
                <div className="pt-4 space-y-3">
                    <a
                        href="/"
                        className="inline-block px-6 py-3 bg-gradient-to-r from-green-500 to-cyan-600 text-white rounded-xl
                                 hover:from-green-600 hover:to-cyan-700 transition-all duration-300 hover:scale-105"
                    >
                        Continue to PetSocial
                    </a>
                </div>

                {/* Background elements */}
                <div className="absolute inset-0 -z-10 overflow-hidden">
                    <div className="absolute -top-20 -right-20 w-96 h-96 bg-green-500/10 rounded-full blur-3xl animate-float"></div>
                    <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl animate-float"
                         style={{ animationDelay: '2s' }}></div>
                </div>
            </div>
        </div>
    )
}