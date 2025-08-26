import { useForm } from "react-hook-form"
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import Input from "@/components/ui/Input"
import Button from "@/components/ui/Button"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"
import { useNavigate, Link } from "react-router-dom"

const schema = z.object({
    username: z.string().min(3, "Too short"),
    password: z.string().min(6, "Min 6 chars"),
})

export default function SignIn() {
    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) })
    const { signin } = useAuth()
    const nav = useNavigate()

    const onSubmit = async (values) => {
        try {
            await signin(values);
            toast.success("🎉 Welcome back!");
            nav("/")
        }
        catch (e) { toast.error(e.message || "🚫 Login failed") }
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="w-full max-w-md mx-auto">
                {/* Glass container */}
                <div className="bg-white/5 backdrop-blur-xl border border-white/10 rounded-3xl p-8 shadow-2xl shadow-cyan-500/10">
                    {/* Animated gradient header */}
                    <div className="text-center mb-8">
                        <h1 className="text-3xl font-bold bg-gradient-to-r from-cyan-400 to-purple-600 bg-clip-text text-transparent animate-glow">
                            Welcome Back
                        </h1>
                        <p className="text-gray-400 mt-2">Sign in to your PetSocial account</p>
                    </div>

                    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
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
                            className="w-full py-3 text-lg font-semibold"
                            variant={isSubmitting ? "ghost" : "primary"}
                        >
                            {isSubmitting ? (
                                <span className="flex items-center gap-2">
                                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                                    Signing in...
                                </span>
                            ) : (
                                "Sign in"
                            )}
                        </Button>
                    </form>

                    <div className="mt-6 pt-6 border-t border-white/10 text-center">
                        <p className="text-gray-400 text-sm">
                            No account?{" "}
                            <Link
                                to="/signup"
                                className="text-cyan-400 hover:text-cyan-300 underline underline-offset-4 transition-colors duration-300"
                            >
                                Sign up now
                            </Link>
                        </p>
                    </div>
                </div>

                {/* Background decorative elements */}
                <div className="absolute inset-0 -z-10 overflow-hidden">
                    <div className="absolute -top-20 -right-20 w-96 h-96 bg-cyan-500/10 rounded-full blur-3xl animate-float"></div>
                    <div className="absolute -bottom-20 -left-20 w-96 h-96 bg-purple-600/10 rounded-full blur-3xl animate-float" style={{ animationDelay: '2s' }}></div>
                </div>
            </div>
        </div>
    )
}