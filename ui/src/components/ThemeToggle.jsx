import { useEffect, useState } from "react"

export default function ThemeToggle() {
    const [theme, setTheme] = useState("light")

    useEffect(() => {
        const saved = localStorage.getItem("theme")
        const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches
        const startTheme = saved || (prefersDark ? "dark" : "light")

        setTheme(startTheme)
        document.documentElement.setAttribute("data-theme", startTheme)
    }, [])

    const toggle = () => {
        const next = theme === "light" ? "dark" : "light"
        setTheme(next)
        document.documentElement.setAttribute("data-theme", next)
        localStorage.setItem("theme", next)
    }

    return (
        <button
            onClick={toggle}
            aria-label={`Switch to ${theme === "light" ? "dark" : "light"} theme`}
            className="relative flex items-center w-16 h-8 rounded-full overflow-hidden transition-all duration-700 ease-in-out
                 bg-gradient-to-r from-yellow-300 via-orange-500 to-pink-500 dark:from-cyan-400 dark:via-blue-500 dark:to-purple-600
                 hover:scale-110 hover:shadow-[0_0_25px_rgba(147,51,234,0.4)]"
        >
            {/* background blur overlay */}
            <div className="absolute inset-0 bg-white/20 dark:bg-white/5 backdrop-blur-md transition-all duration-700"></div>

            {/* thumb */}
            <div
                className={`absolute top-1 left-1 w-6 h-6 rounded-full shadow-lg transition-all duration-700 ease-in-out
                    flex items-center justify-center text-base ${
                    theme === "dark"
                        ? "translate-x-[27px] bg-gradient-to-r from-gray-900 to-gray-700 text-yellow-200"
                        : "translate-x-0 bg-gradient-to-r from-white to-gray-200 text-yellow-500"
                }`}
            >
        <span className="transition-transform duration-500 hover:scale-125">
          {theme === "dark" ? "🌙" : "☀️"}
        </span>
            </div>

            {/* ambient glow */}
            <div
                className={`absolute -inset-1 rounded-full blur-md transition-all duration-700 ${
                    theme === "dark"
                        ? "bg-cyan-400/30 shadow-[0_0_25px_rgba(56,189,248,0.4)]"
                        : "bg-yellow-300/30 shadow-[0_0_25px_rgba(250,204,21,0.4)]"
                }`}
            ></div>
        </button>
    )
}
