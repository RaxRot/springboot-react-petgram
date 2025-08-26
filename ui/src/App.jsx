import { useEffect } from "react"
import { Route, Routes } from "react-router-dom"
import Navbar from "@/components/Navbar"
import { useAuth } from "@/store/auth"
import HomeFeed from "@/pages/HomeFeed"
import SignIn from "@/pages/auth/SignIn"
import SignUp from "@/pages/auth/SignUp"
import CreatePost from "@/pages/posts/CreatePost"
import PostDetails from "@/pages/posts/PostDetails"
import Bookmarks from "@/pages/Bookmarks"
import Profile from "@/pages/Profile"
import AdminUsers from "@/pages/admin/AdminUsers"
import Settings from "@/pages/Settings"
import { Protected, AdminOnly } from "@/components/Protected"
import PaymentSuccess from "@/pages/payments/PaymentSuccess"
import PaymentCancel from "@/pages/payments/PaymentCancel"

// NEW
import MessagesList from "@/pages/messages/MessagesList"
import Chat from "@/pages/messages/Chat"

export default function App() {
    const { me } = useAuth()
    useEffect(() => { me() }, [])

    return (
        <div className="min-h-dvh flex flex-col">
            <Navbar />
            <main className="max-w-6xl mx-auto px-4 py-6 w-full">
                <Routes>
                    <Route path="/" element={<HomeFeed/>} />
                    <Route path="/signin" element={<SignIn/>} />
                    <Route path="/signup" element={<SignUp/>} />
                    <Route path="/posts/:id" element={<PostDetails/>} />
                    <Route path="/profile/:username" element={<Profile/>} />
                    <Route path="/u/:username" element={<Profile/>} />
                    <Route path="/payment-success" element={<PaymentSuccess/>} />
                    <Route path="/payment-cancel" element={<PaymentCancel/>} />

                    <Route element={<Protected />}>
                        <Route path="/create" element={<CreatePost/>} />
                        <Route path="/bookmarks" element={<Bookmarks/>} />
                        <Route path="/settings" element={<Settings/>} />

                        {/* NEW: messages */}
                        <Route path="/messages" element={<MessagesList/>} />
                        <Route path="/messages/:username" element={<Chat/>} />
                    </Route>

                    <Route element={<AdminOnly />}>
                        <Route path="/admin/users" element={<AdminUsers/>} />
                    </Route>
                </Routes>
            </main>
        </div>
    )
}
