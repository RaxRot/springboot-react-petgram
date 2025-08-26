import { Link, NavLink, useNavigate } from "react-router-dom"
import { useAuth } from "@/store/auth"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import UserSearch from "@/components/ui/UserSearch"

export default function Navbar() {
    const { user, isAdmin, signout } = useAuth()
    const nav = useNavigate()
    const logout = async () => { await signout(); toast.info("Logged out"); nav("/signin") }

    const tab = ({ isActive }) => isActive
        ? "relative text-white font-semibold bg-gradient-to-r from-cyan-400/20 to-purple-600/20 px-3 py-1 rounded-lg backdrop-blur-sm border border-white/10 shadow-lg shadow-cyan-500/10"
        : "text-gray-300 hover:text-white px-3 py-1 rounded-lg transition-all duration-300 hover:bg-white/5 backdrop-blur-sm hover:shadow-lg hover:shadow-cyan-500/5"

    return (
        <header className="fixed top-0 w-full z-50">
            {/* Background blur effect */}
            <div className="absolute inset-0 bg-black/80 backdrop-blur-xl border-b border-white/10"></div>

            <div className="relative max-w-7xl mx-auto px-6 h-20 flex items-center gap-6">
                {/* Logo with glow effect */}
                <Link
                    to="/"
                    className="font-bold text-2xl bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent animate-glow"
                >
                    🐾 PetSocial
                </Link>

                <nav className="ml-auto flex items-center gap-1">
                    <NavLink to="/" className={tab}>Feed</NavLink>
                    <NavLink to="/create" className={tab}>Create</NavLink>
                    <NavLink to="/bookmarks" className={tab}>Bookmarks</NavLink>
                    <NavLink to="/messages" className={tab}>Messages</NavLink>
                    {isAdmin() && <NavLink to="/admin/users" className={tab}>Admin</NavLink>}

                    <UserSearch />

                    {user ? (
                        <>
                            <NavLink
                                to={`/u/${user.username}`}
                                className="flex items-center gap-2 text-gray-300 hover:text-white transition-all duration-300 group"
                            >
                                <div className="w-8 h-8 bg-gradient-to-r from-cyan-400 to-purple-600 rounded-full flex items-center justify-center text-white font-bold text-sm">
                                    {user.username?.[0]?.toUpperCase()}
                                </div>
                                <span className="group-hover:scale-105 transition-transform">@{user.username}</span>
                            </NavLink>

                            <NavLink to="/settings" className={tab}>Settings</NavLink>

                            <Button
                                onClick={logout}
                                className="px-4 py-2 bg-gradient-to-r from-red-500 to-pink-600 hover:from-red-600 hover:to-pink-700 text-white border-0 shadow-lg shadow-red-500/30 hover:shadow-red-500/50 transition-all duration-300 hover:scale-105"
                            >
                                Logout
                            </Button>
                        </>
                    ) : (
                        <>
                            <NavLink
                                to="/signin"
                                className="px-4 py-2 text-gray-300 hover:text-white transition-all duration-300 hover:bg-white/5 rounded-lg backdrop-blur-sm"
                            >
                                Sign in
                            </NavLink>
                            <NavLink
                                to="/signup"
                                className="px-4 py-2 bg-gradient-to-r from-cyan-400 to-purple-600 text-white rounded-lg border-0 shadow-lg shadow-cyan-500/30 hover:shadow-cyan-500/50 transition-all duration-300 hover:scale-105"
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