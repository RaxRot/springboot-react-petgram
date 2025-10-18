import { useState } from "react"
import Input from "@/components/ui/Input"
import Button from "@/components/ui/Button"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import { useAuth } from "@/store/auth"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"

export default function Settings() {
    const { signout } = useAuth()
    const nav = useNavigate()
    const qc = useQueryClient()

    const [username, setUsername] = useState("")
    const [currentPassword, setCur] = useState("")
    const [newPassword, setNewp] = useState("")
    const [confirm, setConfirm] = useState("")

    const changeUsername = async (e) => {
        e.preventDefault()
        await toast.promise(
            api.patch("/api/user/username", { newUsername: username }),
            {
                loading: "Saving username…",
                success: "✅ Username updated. Please sign in again.",
                error: "🚫 Failed to update username",
            }
        )

        qc.clear()
        try {
            await api.post("/api/auth/signout")
        } catch {}
        await signout()
        nav("/signin", { replace: true })
    }

    const changePassword = async (e) => {
        e.preventDefault()
        await toast.promise(
            api.patch("/api/user/password", {
                currentPassword,
                newPassword,
                confirmPassword: confirm,
            }),
            {
                loading: "Updating password…",
                success: "✅ Password changed. Please sign in again.",
                error: "🚫 Failed to change password",
            }
        )
        qc.clear()
        await signout()
        nav("/signin", { replace: true })
    }

    return (
        <div className="relative min-h-screen py-8 px-4">
            <div className="max-w-4xl mx-auto space-y-8">

                {/* Header */}
                <div className="text-center">
                    <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                        Account Settings
                    </h1>
                    <p className="text-[hsl(var(--muted-foreground))] mt-2">
                        Manage your profile and security
                    </p>
                </div>

                {/* Two panels */}
                <div className="grid md:grid-cols-2 gap-8">

                    {/* Username */}
                    <div className="rounded-3xl p-6 border border-[hsl(var(--border))]
                        bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                        backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
                        transition-all duration-300">
                        <form onSubmit={changeUsername} className="space-y-4">
                            <div className="text-center mb-2">
                                <div className="text-3xl mb-2">👤</div>
                                <h2 className="text-xl font-semibold">Change Username</h2>
                            </div>

                            <Input
                                placeholder="Enter new username"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                className="w-full"
                                required
                            />

                            <Button
                                type="submit"
                                className="w-full py-3 bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_25px_hsl(var(--ring))]"
                                disabled={!username.trim()}
                            >
                                Update Username
                            </Button>
                        </form>
                    </div>

                    {/* Password */}
                    <div className="rounded-3xl p-6 border border-[hsl(var(--border))]
                        bg-[hsl(var(--card))] text-[hsl(var(--foreground))]
                        backdrop-blur-xl shadow-[0_0_25px_rgba(56,189,248,0.1)]
                        transition-all duration-300">
                        <form onSubmit={changePassword} className="space-y-4">
                            <div className="text-center mb-2">
                                <div className="text-3xl mb-2">🔒</div>
                                <h2 className="text-xl font-semibold">Change Password</h2>
                            </div>

                            <Input
                                type="password"
                                placeholder="Current password"
                                value={currentPassword}
                                onChange={(e) => setCur(e.target.value)}
                                className="w-full"
                                required
                            />
                            <Input
                                type="password"
                                placeholder="New password"
                                value={newPassword}
                                onChange={(e) => setNewp(e.target.value)}
                                className="w-full"
                                required
                            />
                            <Input
                                type="password"
                                placeholder="Confirm new password"
                                value={confirm}
                                onChange={(e) => setConfirm(e.target.value)}
                                className="w-full"
                                required
                            />

                            <Button
                                type="submit"
                                className="w-full py-3 bg-gradient-to-r from-cyan-400 to-purple-600 hover:shadow-[0_0_25px_hsl(var(--ring))]"
                                disabled={!currentPassword || !newPassword || !confirm || newPassword !== confirm}
                            >
                                Update Password
                            </Button>
                        </form>
                    </div>
                </div>

                {/* Info */}
                <div className="text-center text-sm text-[hsl(var(--muted-foreground))]">
                    ⚠️ Changing your credentials will log you out from all devices
                </div>
            </div>

            {/* Background glow */}
            <div className="absolute inset-0 -z-10 overflow-hidden">
                <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
            </div>
        </div>
    )
}
