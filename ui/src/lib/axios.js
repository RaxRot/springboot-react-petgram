import axios from "axios"
import { toast } from "sonner"

export const api = axios.create({
    baseURL: "http://localhost:8080",
    withCredentials: true,
})

api.interceptors.response.use(
    (r) => r,
    (err) => {
        const raw = err?.response?.data
        const msg = raw?.message || raw || err.message

        if (
            msg === "Poll not found" ||
            raw === "Poll not found" ||
            err?.response?.status === 404
        ) {
            // просто молча пропускаем без toast
            return Promise.reject(err)
        }

        if (err?.response?.status !== 401) {
            toast.error(msg)
        }

        return Promise.reject(err)
    }
)
