import { defineConfig } from "vite"
import react from "@vitejs/plugin-react"
import { fileURLToPath, URL } from "node:url"
import tailwindcss from "@tailwindcss/vite"

import forms from "@tailwindcss/forms"
import typography from "@tailwindcss/typography"

export default defineConfig({
  plugins: [
    tailwindcss({
      plugins: [forms, typography],

      theme: {
        extend: {
          colors: {
            dark: {
              bg: "#0d1117",
              card: "#161b22",
              accent: "#1f2937",
            },
            neon: {
              dog: "#06b6d4", // 🐶 голубой
              cat: "#ec4899", // 🐱 розовый
              bird: "#facc15", // 🐦 жёлтый
              fish: "#3b82f6", // 🐟 синий
              pig: "#f87171", // 🐷 коралловый
              other: "#a855f7", // 🦄 фиолетовый
            },
          },

          backgroundImage: {
            "dark-gradient":
                "radial-gradient(ellipse at top, #1a1f2b 0%, #0d1117 100%)",
          },

          animation: {
            float: "float 6s ease-in-out infinite",
            glow: "glow 2s ease-in-out infinite alternate",
          },

          keyframes: {
            float: {
              "0%, 100%": { transform: "translateY(0px)" },
              "50%": { transform: "translateY(-10px)" },
            },
            glow: {
              "0%": {
                boxShadow:
                    "0 0 5px #ff00ff, 0 0 10px #ff00ff, 0 0 15px #ff00ff",
              },
              "100%": {
                boxShadow:
                    "0 0 10px #00ffff, 0 0 20px #00ffff, 0 0 30px #00ffff",
              },
            },
          },
        },
      },
    }),
    react(),
  ],

  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },

  server: {
    historyApiFallback: true,
    port: 5173,
  },
})
