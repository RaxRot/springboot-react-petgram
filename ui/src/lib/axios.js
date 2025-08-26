import axios from "axios"
import { toast } from "sonner"

export const api = axios.create({
    baseURL: "http://localhost:8080",
    withCredentials: true,
})

api.interceptors.response.use(
    (r) => r,
    (err) => {
        const msg = err?.response?.data?.message || err.message
        if (err?.response?.status !== 401) toast.error(msg)
        return Promise.reject(new Error(msg))
    }
)
