import { useEffect } from "react";
import { Route, Routes } from "react-router-dom";
import Navbar from "@/components/Navbar";
import { useAuth } from "@/store/auth";
import HomeFeed from "@/pages/HomeFeed";
import SignIn from "@/pages/auth/SignIn";
import SignUp from "@/pages/auth/SignUp";
import CreatePost from "@/pages/posts/CreatePost";
import PostDetails from "@/pages/posts/PostDetails";
import Bookmarks from "@/pages/Bookmarks";
import Profile from "@/pages/Profile";
import Settings from "@/pages/Settings";
import AdminUsers from "@/pages/admin/AdminUsers";
import AdminDonations from "@/pages/admin/AdminDonations";
import AdminComments from "@/pages/admin/AdminComments";
import AdminDashboard from "@/pages/admin/AdminDashboard";
import { Protected, AdminOnly } from "@/components/Protected";
import PaymentSuccess from "@/pages/payments/PaymentSuccess";
import PaymentCancel from "@/pages/payments/PaymentCancel";
import MessagesList from "@/pages/messages/MessagesList";
import Chat from "@/pages/messages/Chat";
import StoryCreate from "@/pages/stories/StoryCreate.jsx";
import StoryView from "@/pages/stories/StoryView.jsx";
import MyPets from "@/pages/MyPets.jsx";
import PetDetails from "@/pages/pets/PetDetails.jsx";
import AdminInsights from "@/pages/admin/AdminInsights.jsx";

export default function App() {
    const { me } = useAuth();

    useEffect(() => {
        me();
    }, []);

    return (
        <div className="min-h-dvh flex flex-col">
            <Navbar />
            <main className="max-w-7xl mx-auto px-6 w-full pt-24 pb-12">
                <Routes>
                    {/* Публичные маршруты */}
                    <Route path="/" element={<HomeFeed />} />
                    <Route path="/signin" element={<SignIn />} />
                    <Route path="/signup" element={<SignUp />} />
                    <Route path="/posts/:id" element={<PostDetails />} />
                    <Route path="/pets/:id" element={<PetDetails />} />
                    <Route path="/profile/:username" element={<Profile />} />
                    <Route path="/u/:username" element={<Profile />} />
                    <Route path="/payment-success" element={<PaymentSuccess />} />
                    <Route path="/payment-cancel" element={<PaymentCancel />} />
                    <Route path="/stories/:id" element={<StoryView />} />

                    {/* Защищённые маршруты */}
                    <Route element={<Protected />}>
                        <Route path="/create" element={<CreatePost />} />
                        <Route path="/bookmarks" element={<Bookmarks />} />
                        <Route path="/settings" element={<Settings />} />
                        <Route path="/messages" element={<MessagesList />} />
                        <Route path="/messages/:username" element={<Chat />} />
                        <Route path="/stories/create" element={<StoryCreate />} />
                        <Route path="/mypets" element={<MyPets />} />
                    </Route>

                    {/* Админские маршруты */}
                    <Route element={<AdminOnly />}>
                        <Route path="/admin/dashboard" element={<AdminDashboard />} />
                        <Route path="/admin/users" element={<AdminUsers />} />
                        <Route path="/admin/donations" element={<AdminDonations />} />
                        <Route path="/admin/comments" element={<AdminComments />} />
                        <Route path="/admin/insights" element={<AdminInsights />} />
                    </Route>
                </Routes>
            </main>
        </div>
    );
}
