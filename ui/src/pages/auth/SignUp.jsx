import { useForm } from "react-hook-form"
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import Input from "@/components/ui/Input"
import Button from "@/components/ui/Button"
import { useAuth } from "@/store/auth"
import { toast } from "sonner"
import { useNavigate, Link } from "react-router-dom"

const schema = z.object({
    username: z.string().min(3),
    email: z.string().email(),
    password: z.string().min(6),
})

export default function SignUp() {
    const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) })
    const { signup } = useAuth()
    const nav = useNavigate()

    const onSubmit = async (values) => {
        try { await signup(values); toast.success("Account created. Please sign in."); nav("/signin") }
        catch (e) { toast.error(e.message || "Signup failed") }
    }

    return (
        <div className="max-w-md mx-auto space-y-4">
            <h1 className="text-2xl font-bold">Sign Up</h1>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-3">
                <Input placeholder="Username" {...register("username")} />
                {errors.username && <p className="text-red-500 text-xs">{errors.username.message}</p>}
                <Input placeholder="Email" {...register("email")} />
                {errors.email && <p className="text-red-500 text-xs">{errors.email.message}</p>}
                <Input type="password" placeholder="Password" {...register("password")} />
                {errors.password && <p className="text-red-500 text-xs">{errors.password.message}</p>}
                <Button disabled={isSubmitting} className="w-full">Sign up</Button>
            </form>
            <p className="text-sm opacity-70">Already have an account? <Link to="/signin" className="underline">Sign in</Link></p>
        </div>
    )
}
