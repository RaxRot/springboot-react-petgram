import { Link, NavLink, useNavigate } from "react-router-dom"
import { useAuth } from "@/store/auth"
import Button from "@/components/ui/Button"
import { toast } from "sonner"
import UserSearch from "@/components/ui/UserSearch"

export default function Navbar() {
    const { user, isAdmin, signout } = useAuth()
    const nav = useNavigate()
    const logout = async () => { await signout(); toast.info("Logged out"); nav("/signin") }

    const tab = ({ isActive }) => isActive ? "text-[--color-primary] font-semibold" : "opacity-70 hover:opacity-100"

    return (
        <header className="border-b border-[--color-border] sticky top-0 bg-white/80 backdrop-blur z-50">
            <div className="max-w-6xl mx-auto px-4 h-16 flex items-center gap-4">
                <Link to="/" className="font-bold text-lg">🐾 PetSocial</Link>

                <nav className="ml-auto flex items-center gap-3">
                    <NavLink to="/" className={tab}>Feed</NavLink>
                    <NavLink to="/create" className={tab}>Create</NavLink>
                    <NavLink to="/bookmarks" className={tab}>Bookmarks</NavLink>
                    <NavLink to="/messages" className={tab}>Messages</NavLink>
                    {isAdmin() && <NavLink to="/admin/users" className={tab}>Admin</NavLink>}

                    <UserSearch />

                    {user ? (
                        <>
                            <NavLink to={`/u/${user.username}`} className={tab}>@{user.username}</NavLink>
                            <NavLink to="/settings" className={tab}>Settings</NavLink>
                            <Button onClick={logout} className="px-3 py-1">Logout</Button>
                        </>
                    ) : (
                        <>
                            <NavLink to="/signin" className={tab}>Sign in</NavLink>
                            <NavLink to="/signup" className={tab}>Sign up</NavLink>
                        </>
                    )}
                </nav>
            </div>
        </header>
    )
}
