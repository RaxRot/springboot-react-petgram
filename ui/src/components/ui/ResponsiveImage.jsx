export default function ResponsiveImage({ src, alt = "", className = "" }) {
    return (
        <img
            src={src}
            alt={alt}
            className={`block w-full h-auto rounded-2xl ${className}`}
        />
    );
}
