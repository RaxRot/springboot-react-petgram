import { Link, NavLink, useNavigate } from "react-router-dom"
import { useAuth } from "@/store/auth"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import UserSearch from "@/components/ui/UserSearch"
import ThemeToggle from "@/components/ThemeToggle"
import { useEffect, useRef, useState } from "react"

export default function Navbar() {
    const { user, isAdmin, signout } = useAuth()
    const nav = useNavigate()
    const [openAdmin, setOpenAdmin] = useState(false)
    const [scrolled, setScrolled] = useState(false)
    const dropdownRef = useRef(null)

    const logout = async () => {
        await signout()
        toast.info("Logged out")
        nav("/signin")
    }

    useEffect(() => {
        const close = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setOpenAdmin(false)
            }
        }
        const onScroll = () => setScrolled(window.scrollY > 20)
        document.addEventListener("mousedown", close)
        window.addEventListener("scroll", onScroll)
        return () => {
            document.removeEventListener("mousedown", close)
            window.removeEventListener("scroll", onScroll)
        }
    }, [])

    const tab = ({ isActive }) =>
        isActive
            ? "relative font-semibold px-3 py-1 rounded-xl bg-gradient-to-r from-cyan-400 to-purple-600 text-white shadow-[0_0_15px_rgba(56,189,248,0.5)]"
            : "text-[hsl(var(--foreground))] hover:text-cyan-400 px-3 py-1 rounded-xl transition-all duration-300 hover:bg-[hsl(var(--muted))]/10"

    return (
        <header
            className={`fixed top-0 w-full z-50 transition-all duration-500 ${
                scrolled
                    ? "backdrop-blur-xl bg-[hsl(var(--background))]/70 border-b border-[hsl(var(--border))] shadow-[0_0_25px_rgba(56,189,248,0.08)]"
                    : "bg-[hsl(var(--background))]/40 border-b border-transparent"
            }`}
        >
            <div className="max-w-7xl mx-auto px-6 sm:px-8 h-20 flex items-center justify-between">
                {/* 🐾 Logo */}
                <Link
                    to="/"
                    className="font-extrabold text-2xl bg-gradient-to-r from-cyan-400 via-pink-500 to-purple-600 bg-clip-text text-transparent hover:scale-110 transition-transform duration-300 drop-shadow-[0_0_10px_rgba(147,51,234,0.3)]"
                >
                    🐾 PetSocial
                </Link>

                {/* 🔗 Navigation */}
                <nav className="flex items-center gap-3">
                    <NavLink to="/" className={tab}>
                        Feed
                    </NavLink>
                    <NavLink to="/create" className={tab}>
                        Create
                    </NavLink>
                    <NavLink to="/bookmarks" className={tab}>
                        Bookmarks
                    </NavLink>
                    <NavLink to="/messages" className={tab}>
                        Messages
                    </NavLink>

                    {/* ⚙️ Admin dropdown */}
                    {isAdmin() && (
                        <div className="relative" ref={dropdownRef}>
                            <button
                                onClick={() => setOpenAdmin(!openAdmin)}
                                className="flex items-center gap-2 px-3 py-1 rounded-xl text-[hsl(var(--foreground))] hover:text-cyan-400 hover:bg-[hsl(var(--muted))]/10 transition-all"
                            >
                                ⚙️ Admin
                                <span
                                    className={`transition-transform duration-300 ${
                                        openAdmin ? "rotate-180" : ""
                                    }`}
                                >
                                    ▼
                                </span>
                            </button>

                            {openAdmin && (
                                <div
                                    className="absolute right-0 mt-3 w-56 rounded-2xl border border-[hsl(var(--border))]
                                    bg-[hsl(var(--card))]/90 backdrop-blur-xl
                                    shadow-[0_0_25px_rgba(56,189,248,0.15)] overflow-hidden z-50"
                                >
                                    {[
                                        { to: "/admin/dashboard", icon: "🧩", label: "Dashboard" },
                                        { to: "/admin/users", icon: "👥", label: "Users" },
                                        { to: "/admin/donations", icon: "💰", label: "Donations" },
                                        { to: "/admin/comments", icon: "💬", label: "Comments" },
                                    ].map((item) => (
                                        <NavLink
                                            key={item.to}
                                            to={item.to}
                                            onClick={() => setOpenAdmin(false)}
                                            className="flex items-center gap-2 px-4 py-2 text-[hsl(var(--foreground))] hover:text-cyan-400 hover:bg-[hsl(var(--muted))]/10 transition-all"
                                        >
                                            <span>{item.icon}</span>
                                            {item.label}
                                        </NavLink>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}

                    <UserSearch />
                    <ThemeToggle />

                    {/* 👤 User */}
                    {user ? (
                        <>
                            <NavLink
                                to={`/u/${user.username}`}
                                className="flex items-center gap-2 text-[hsl(var(--foreground))] hover:text-cyan-400 transition-all"
                            >
                                <div className="w-8 h-8 rounded-full bg-gradient-to-r from-cyan-400 to-purple-600 flex items-center justify-center text-white font-bold text-sm shadow-[0_0_10px_rgba(56,189,248,0.3)]">
                                    {user.username?.[0]?.toUpperCase()}
                                </div>
                                <span>@{user.username}</span>
                            </NavLink>

                            <NavLink to="/settings" className={tab}>
                                Settings
                            </NavLink>

                            <Button
                                onClick={logout}
                                className="px-4 py-2 bg-gradient-to-r from-red-500 to-pink-600 hover:from-red-600 hover:to-pink-700 text-white rounded-xl shadow-[0_0_15px_rgba(239,68,68,0.3)] hover:shadow-[0_0_25px_rgba(239,68,68,0.5)] hover:scale-105 transition-all font-medium"
                            >
                                Logout
                            </Button>
                        </>
                    ) : (
                        <>
                            <NavLink
                                to="/signin"
                                className="px-4 py-2 text-[hsl(var(--foreground))] hover:text-cyan-400 hover:bg-[hsl(var(--muted))]/10 rounded-xl transition-all duration-300 font-medium"
                            >
                                Sign in
                            </NavLink>
                            <NavLink
                                to="/signup"
                                className="px-4 py-2 bg-gradient-to-r from-cyan-400 to-purple-600 text-white rounded-xl shadow-[0_0_20px_rgba(56,189,248,0.4)] hover:shadow-[0_0_30px_rgba(56,189,248,0.6)] hover:scale-105 transition-all duration-300 font-medium"
                            >
                                Sign up
                            </NavLink>
                        </>
                    )}
                </nav>
            </div>
        </header>
    )
}
