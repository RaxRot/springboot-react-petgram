
import React from "react"
import ReactDOM from "react-dom/client"
import { BrowserRouter } from "react-router-dom"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { Toaster } from "sonner"
import App from "./App.jsx"
import "./index.css"

const qc = new QueryClient()

ReactDOM.createRoot(document.getElementById("root")).render(
    <React.StrictMode>
        <QueryClientProvider client={qc}>
            <BrowserRouter>
                <App />
                <Toaster richColors closeButton />
            </BrowserRouter>
        </QueryClientProvider>
    </React.StrictMode>
)
