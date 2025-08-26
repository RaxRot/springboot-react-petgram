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
        try { await api.post("/api/auth/signout") } catch {}
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
        <div className="min-h-screen py-8 px-4">
            <div className="max-w-4xl mx-auto">
                {/* Header */}
                <div className="text-center mb-8">
                    <h1 className="text-4xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent">
                        Account Settings
                    </h1>
                    <p className="text-gray-600 dark:text-gray-400 mt-2">Manage your profile and security</p>
                </div>

                <div className="grid md:grid-cols-2 gap-8">
                    {/* Change Username */}
                    <div className="bg-white dark:bg-white/5 backdrop-blur-xl rounded-3xl p-6 border border-gray-200 dark:border-white/10 shadow-2xl shadow-cyan-500/10">
                        <form onSubmit={changeUsername} className="space-y-4">
                            <div className="text-center mb-4">
                                <div className="text-3xl mb-2">👤</div>
                                <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Change Username</h2>
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
                                className="w-full py-3 bg-gradient-to-r from-cyan-400 to-purple-600"
                                disabled={!username.trim()}
                            >
                                Update Username
                            </Button>
                        </form>
                    </div>

                    {/* Change Password */}
                    <div className="bg-white dark:bg-white/5 backdrop-blur-xl rounded-3xl p-6 border border-gray-200 dark:border-white/10 shadow-2xl shadow-cyan-500/10">
                        <form onSubmit={changePassword} className="space-y-4">
                            <div className="text-center mb-4">
                                <div className="text-3xl mb-2">🔒</div>
                                <h2 className="text-xl font-semibold text-gray-900 dark:text-white">Change Password</h2>
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
                                className="w-full py-3 bg-gradient-to-r from-cyan-400 to-purple-600"
                                disabled={!currentPassword || !newPassword || !confirm || newPassword !== confirm}
                            >
                                Update Password
                            </Button>
                        </form>
                    </div>
                </div>

                {/* Additional Info */}
                <div className="mt-8 text-center text-sm text-gray-500 dark:text-gray-400">
                    <p>⚠️ Changing your credentials will log you out from all devices</p>
                </div>

                {/* Background elements */}
                <div className="absolute inset-0 -z-10 overflow-hidden">
                    <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl"></div>
                    <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl"></div>
                </div>
            </div>
        </div>
    )
}