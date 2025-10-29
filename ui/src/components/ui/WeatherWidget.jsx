import { useEffect, useState } from "react"
import { api } from "@/lib/axios"

export default function WeatherWidget() {
    const [weather, setWeather] = useState("🌍 Loading...")

    useEffect(() => {
        if (!navigator.geolocation) {
            setWeather("❔ Location unavailable")
            return
        }

        navigator.geolocation.getCurrentPosition(
            async (pos) => {
                try {
                    const { latitude, longitude } = pos.coords
                    const res = await api.get(`/api/weather?lat=${latitude}&lon=${longitude}`)
                    setWeather(res.data.weather)
                } catch {
                    setWeather("⚠️ Error loading weather")
                }
            },
            () => setWeather("📍 Permission denied")
        )
    }, [])

    return (
        <div className="flex items-center gap-2 px-3 py-1 rounded-xl
                    bg-[hsl(var(--muted))]/20 text-sm font-medium">
            {weather}
        </div>
    )
}
