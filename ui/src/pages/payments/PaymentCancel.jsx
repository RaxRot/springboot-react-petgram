export default function PaymentCancel() {
    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="max-w-md mx-auto text-center space-y-6">
                {/* Animated icon */}
                <div className="text-6xl animate-bounce">❌</div>

                <h1 className="text-3xl font-bold bg-gradient-to-r from-red-400 to-orange-600 bg-clip-text text-transparent">
                    Payment Canceled
                </h1>

                <p className="text-gray-600 dark:text-gray-400 text-lg">
                    No worries! You can try again whenever you're ready.
                </p>

                {/* Action buttons */}
                <div className="pt-4 space-y-3">
                    <a
                        href="/"
                        className="inline-block px-6 py-3 bg-gradient-to-r from-gray-600 to-gray-800 text-white rounded-xl
                                 hover:from-gray-700 hover:to-gray-900 transition-all duration-300 hover:scale-105"
                    >
                        Go Home
                    </a>
                </div>

                {/* Background elements */}
                <div className="absolute inset-0 -z-10 overflow-hidden">
                    <div className="absolute -top-20 -left-20 w-96 h-96 bg-red-500/10 rounded-full blur-3xl"></div>
                    <div className="absolute -bottom-20 -right-20 w-96 h-96 bg-orange-500/10 rounded-full blur-3xl"></div>
                </div>
            </div>
        </div>
    )
}