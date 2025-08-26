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
        try { await signin(values); toast.success("Welcome back!"); nav("/") }
        catch (e) { toast.error(e.message || "Login failed") }
    }

    return (
        <div className="max-w-md mx-auto space-y-4">
            <h1 className="text-2xl font-bold">Sign In</h1>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
                <div>
                    <Input placeholder="Username" {...register("username")} />
                    {errors.username && <p className="text-red-500 text-xs mt-1">{errors.username.message}</p>}
                </div>
                <div>
                    <Input type="password" placeholder="Password" {...register("password")} />
                    {errors.password && <p className="text-red-500 text-xs mt-1">{errors.password.message}</p>}
                </div>
                <Button disabled={isSubmitting} className="w-full">Sign in</Button>
            </form>
            <p className="text-sm opacity-70">No account? <Link to="/signup" className="underline">Sign up</Link></p>
        </div>
    )
}
