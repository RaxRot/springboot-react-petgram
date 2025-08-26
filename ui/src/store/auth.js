// src/store/auth.js
import { create } from "zustand"
import { api } from "@/lib/axios"

export const useAuth = create((set, get) => ({
    user: null,         // { id, username, email }
    roles: [],          // ["ROLE_USER", "ROLE_ADMIN", ...]
    loading: false,
    ready: false,       // true после первой попытки me()

    isAuthed: () => !!get().user,
    isAdmin: () => get().roles?.includes("ROLE_ADMIN"),

    me: async () => {
        set({ loading: true })
        try {
            const { data } = await api.get("/api/auth/user")
            set({
                user: { id: data.id, username: data.username, email: data.email },
                roles: data.roles || [],
            })
        } catch (e) {
            // без куки/не залогинен → 401 → просто гость
            if (e?.response?.status === 401) {
                set({ user: null, roles: [] })
            } else {
                // на всякий лог — чтобы видеть неожиданные 5xx/4xx
                console.error("me() failed:", e)
                set({ user: null, roles: [] })
            }
        } finally {
            set({ loading: false, ready: true })
        }
    },

    signin: async (payload) => {
        await api.post("/api/auth/signin", payload)
        await get().me()
    },

    signup: async (payload) => {
        await api.post("/api/auth/signup", payload)
        // при желании можно сразу авторизовывать или редиректить на signin
    },

    signout: async () => {
        await api.post("/api/auth/signout")
        set({ user: null, roles: [] })
    },
}))
