import { useForm } from "react-hook-form"
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import Input from "@/components/ui/Input"
import Button from "@/components/ui/Button"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"
import { useNavigate, Link } from "react-router-dom"

const schema = z.object({
    username: z.string().min(3, "Username must be at least 3 characters"),
    email: z.string().email("Please enter a valid email"),
    password: z.string().min(6, "Password must be at least 6 characters"),
})

export default function SignUp() {
    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) })
    const { signup } = useAuth()
    const nav = useNavigate()

    const onSubmit = async (values) => {
        try {
            await signup(values);
            toast.success("🎉 Account created. Please sign in.");
            nav("/signin")
        }
        catch (e) { toast.error(e.message || "🚫 Signup failed") }
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="w-full max-w-md mx-auto">
                {/* Glass container */}
                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-8 shadow-2xl shadow-purple-500/10">
                    {/* Animated gradient header */}
                    <div className="text-center mb-8">
                        <h1 className="text-3xl font-bold bg-gradient-to-r from-purple-400 to-pink-600 bg-clip-text text-transparent animate-glow">
                            Join PetSocial
                        </h1>
                        <p className="text-gray-400 mt-2">Create your account and start sharing pet moments</p>
                    </div>

                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
                        <div className="space-y-2">
                            <Input
                                placeholder="Username"
                                {...register("username")}
                                className="w-full"
                            />
                            {errors.username && (
                                <p className="text-red-400 text-sm mt-1 flex items-center gap-1">
                                    <span>⚠️</span>
                                    {errors.username.message}
                                </p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <Input
                                placeholder="Email"
                                {...register("email")}
                                className="w-full"
                            />
                            {errors.email && (
                                <p className="text-red-400 text-sm mt-1 flex items-center gap-1">
                                    <span>⚠️</span>
                                    {errors.email.message}
                                </p>
                            )}
                        </div>

                        <div className="space-y-2">
                            <Input
                                type="password"
                                placeholder="Password"
                                {...register("password")}
                                className="w-full"
                            />
                            {errors.password && (
                                <p className="text-red-400 text-sm mt-1 flex items-center gap-1">
                                    <span>⚠️</span>
                                    {errors.password.message}
                                </p>
                            )}
                        </div>

                        <Button
                            disabled={isSubmitting}
                            className="w-full py-3 text-lg font-semibold mt-4"
                            variant={isSubmitting ? "ghost" : "primary"}
                        >
                            {isSubmitting ? (
                                <span className="flex items-center gap-2">
                                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                                    Creating account...
                                </span>
                            ) : (
                                <span className="flex items-center gap-2">
                                    <span>🐾</span>
                                    Sign up
                                </span>
                            )}
                        </Button>
                    </form>

                    <div className="mt-6 pt-6 border-t border-white/10 text-center">
                        <p className="text-gray-400 text-sm">
                            Already have an account?{" "}
                            <Link
                                to="/signin"
                                className="text-purple-400 hover:text-purple-300 underline underline-offset-4 transition-colors duration-300"
                            >
                                Sign in
                            </Link>
                        </p>
                    </div>
                </div>

                {/* Background decorative elements */}
                <div className="absolute inset-0 -z-10 overflow-hidden">
                    <div className="absolute -top-20 -left-20 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl animate-float"></div>
                    <div className="absolute -bottom-20 -right-20 w-96 h-96 bg-pink-600/10 rounded-full blur-3xl animate-float" style={{ animationDelay: '2s' }}></div>
                </div>
            </div>
        </div>
    )
}