import { useState } from 'react'

type DoubleConfirmProps = {
  title: string
  firstMessage: string
  secondMessage: string
  confirmDanger?: boolean
  onConfirm: () => void
  onCancel: () => void
}

/**
 * Modal de doble confirmación. El segundo paso invierte Sí/No para evitar clics automáticos.
 */
export function DoubleConfirm({
  title,
  firstMessage,
  secondMessage,
  confirmDanger = false,
  onConfirm,
  onCancel,
}: DoubleConfirmProps) {
  const [step, setStep] = useState<1 | 2>(1)
  const yes = (
    <button
      type="button"
      className={step === 2 && confirmDanger ? 'danger' : undefined}
      onClick={() => {
        if (step === 1) {
          setStep(2)
        } else {
          onConfirm()
        }
      }}
    >
      Sí
    </button>
  )
  const no = (
    <button type="button" className="secondary" onClick={onCancel}>
      No
    </button>
  )

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-title"
        onClick={(event) => event.stopPropagation()}
      >
        <h2 id="confirm-title">{title}</h2>
        <p>{step === 1 ? firstMessage : secondMessage}</p>
        <div className="modal-actions">
          {step === 1 ? (
            <>
              {yes}
              {no}
            </>
          ) : (
            <>
              {no}
              {yes}
            </>
          )}
        </div>
      </div>
    </div>
  )
}
