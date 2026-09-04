type VariantThumbProps = {
  imageUrl: string | null
}

/** Miniatura a la izquierda de cada variante. Sin foto = icono de cámara. */
export function VariantThumb({ imageUrl }: VariantThumbProps) {
  if (imageUrl) {
    return <img src={imageUrl} alt="" className="thumb" />
  }
  return (
    <span className="thumb thumb-empty" title="Sin foto">
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
          fill="currentColor"
          d="M9 3 7.2 5H4a2 2 0 0 0-2 2v11a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-3.2L15 3H9Zm3 5a5 5 0 1 1 0 10 5 5 0 0 1 0-10Zm0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"
        />
      </svg>
    </span>
  )
}
