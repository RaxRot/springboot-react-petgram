import { toast } from "sonner"

// usage: const ok = await confirmToast({ title:"Delete?", desc:"Cannot be undone", okText:"Delete" })
export function confirmToast({ title, desc, okText = "Confirm", cancelText = "Cancel", duration = 5000 }) {
    return new Promise((resolve) => {
        const id = toast(
            (t) => (
                <div className="space-y-2">
                    <div className="font-medium">{title}</div>
                    {desc && <div className="text-sm opacity-80">{desc}</div>}
                    <div className="flex gap-2 pt-1">
                        <button
                            onClick={() => { toast.dismiss(t); resolve(true) }}
                            className="px-3 py-1 rounded-md bg-black text-white text-sm"
                        >
                            {okText}
                        </button>
                        <button
                            onClick={() => { toast.dismiss(t); resolve(false) }}
                            className="px-3 py-1 rounded-md border text-sm"
                        >
                            {cancelText}
                        </button>
                    </div>
                </div>
            ),
            { duration }
        )
    })
}
