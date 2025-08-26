// Settings.jsx
import { useState } from "react"
import Input from "@/components/ui/Input"
import Button from "@/components/ui/Button"
import { api } from "@/lib/axios"
import { toast } from "sonner"
import { useAuth } from "@/store/auth"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"

export default function Settings() {
    const { signout } = useAuth()          // <-- добавили
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
                success: "Username updated. Please sign in again.",
                error: "Failed to update username",
            }
        )

        // подчистим весь клиентский кеш (на всякий)
        qc.clear()

        // серверный cookie всё ещё может быть старым — попросим сервер его очистить
        try { await api.post("/api/auth/signout") } catch {}

        // и локальный стейт/навигация
        await signout()
        nav("/signin", { replace: true })
        // при желании — «жёсткий» ресет страницы:
        // window.location.replace("/signin")
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
                success: "Password changed. Please sign in again.",
                error: "Failed to change password",
            }
        )
        qc.clear()
        await signout()
        nav("/signin", { replace: true })
    }

    return (
        <div className="grid md:grid-cols-2 gap-8 max-w-3xl mx-auto">
            <form onSubmit={changeUsername} className="space-y-3">
                <h2 className="text-xl font-semibold">Change username</h2>
                <Input
                    placeholder="New username"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                />
                <Button type="submit">Save</Button>
            </form>

            <form onSubmit={changePassword} className="space-y-3">
                <h2 className="text-xl font-semibold">Change password</h2>
                <Input
                    type="password"
                    placeholder="Current password"
                    value={currentPassword}
                    onChange={(e) => setCur(e.target.value)}
                />
                <Input
                    type="password"
                    placeholder="New password"
                    value={newPassword}
                    onChange={(e) => setNewp(e.target.value)}
                />
                <Input
                    type="password"
                    placeholder="Confirm new password"
                    value={confirm}
                    onChange={(e) => setConfirm(e.target.value)}
                />
                <Button type="submit">Save</Button>
            </form>
        </div>
    )
}
