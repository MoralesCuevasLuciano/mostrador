import { useEffect, useState } from 'react'
import { DoubleConfirm } from '../components/DoubleConfirm'
import type { Branch } from '../models/branch'
import type { CashMovement, CashMovementType, CashSession } from '../models/cash'
import {
  closeCashSession,
  countCashOpening,
  deleteCashMovement,
  fetchCashMovements,
  fetchCashSession,
  fetchCashSessions,
  fetchCashToday,
  fetchOpenCashSessions,
  fetchPreviousCashSession,
  registerCashMovement,
  updateCashMovement,
} from '../services/cashService'
import {
  CASH_MOVEMENT_TYPES,
  cashMovementLabel,
  differenceLabel,
  formatBusinessDate,
  formatMovementAt,
  formatWeekRange,
  parseMoney,
  shiftWeek,
  todayIso,
  weekContaining,
} from '../utils/cash'
import { formatPrice } from '../utils/money'

type CashPageProps = {
  branch: Branch | null
}

/** Qué está pidiendo confirmación: cierre de hoy, caja trabada o borrar un movimiento. */
type Pending =
  | { kind: 'close' }
  | { kind: 'closeStuck' }
  | { kind: 'delete'; movement: CashMovement }

/** Planilla del día: abrir, anotar movimientos, recuento y cierre. */
export function CashPage({ branch }: CashPageProps) {
  const [deskReady, setDeskReady] = useState(false)
  const [session, setSession] = useState<CashSession | null>(null)
  const [previous, setPrevious] = useState<CashSession | null>(null)
  const [stuck, setStuck] = useState<CashSession | null>(null)
  const [movements, setMovements] = useState<CashMovement[]>([])
  const [historyFrom, setHistoryFrom] = useState(() => weekContaining(todayIso()).from)
  const [history, setHistory] = useState<CashSession[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [countAmount, setCountAmount] = useState('')
  const [showCount, setShowCount] = useState(false)
  const [closeAmount, setCloseAmount] = useState('')
  const [closeNote, setCloseNote] = useState('')
  const [stuckAmount, setStuckAmount] = useState('')
  const [moveType, setMoveType] = useState<CashMovementType>('RETIRO_RESGUARDO')
  const [moveAmount, setMoveAmount] = useState('')
  const [moveDescription, setMoveDescription] = useState('')
  const [editing, setEditing] = useState<CashMovement | null>(null)
  const [editType, setEditType] = useState<CashMovementType>('RETIRO_RESGUARDO')
  const [editAmount, setEditAmount] = useState('')
  const [editDescription, setEditDescription] = useState('')
  const [pending, setPending] = useState<Pending | null>(null)
  const [historyDate, setHistoryDate] = useState<string | null>(null)
  const [historyMovements, setHistoryMovements] = useState<CashMovement[] | null>(null)

  /** Carga la planilla de hoy, la anterior y si hay una caja trabada. No abre nada. */
  async function loadDesk(branchId: number) {
    setError(null)
    try {
      const today = todayIso()
      const [todaySession, openSessions, previousSession] = await Promise.all([
        fetchCashSession(branchId, today),
        fetchOpenCashSessions(branchId),
        fetchPreviousCashSession(branchId, today),
      ])
      setSession(todaySession)
      setPrevious(previousSession)
      setStuck(openSessions.find((item) => item.businessDate !== today) ?? null)
      setDeskReady(true)
      if (todaySession) {
        setMovements(await fetchCashMovements(branchId, todaySession.businessDate))
      } else {
        setMovements([])
      }
    } catch (err) {
      setDeskReady(false)
      setSession(null)
      setPrevious(null)
      setStuck(null)
      setMovements([])
      setError(err instanceof Error ? err.message : 'No se pudo cargar la caja')
    }
  }

  /** Carga las planillas de la semana visible. */
  async function loadHistory(branchId: number, from: string) {
    const week = weekContaining(from)
    try {
      const list = await fetchCashSessions(branchId, week.from, week.to)
      setHistory(list.filter((item) => item.businessDate !== todayIso()))
    } catch (err) {
      setHistory([])
      setError(err instanceof Error ? err.message : 'No se pudo cargar el historial')
    }
  }

  /** Recarga mostrador e historial después de un cambio. */
  async function reload(branchId: number) {
    await loadDesk(branchId)
    await loadHistory(branchId, historyFrom)
  }

  useEffect(() => {
    if (branch == null) {
      setDeskReady(false)
      setSession(null)
      setPrevious(null)
      setStuck(null)
      setHistory(null)
      return
    }
    const currentWeek = weekContaining(todayIso()).from
    setHistoryFrom(currentWeek)
    setDeskReady(false)
    setHistory(null)
    void loadDesk(branch.id)
  }, [branch?.id])

  useEffect(() => {
    if (branch == null) {
      return
    }
    void loadHistory(branch.id, historyFrom)
  }, [branch?.id, historyFrom])

  /** Abre la planilla de hoy heredando el cierre anterior. */
  async function openToday() {
    if (!branch) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      await fetchCashToday(branch.id)
      await reload(branch.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo abrir la caja')
      await reload(branch.id)
    } finally {
      setBusy(false)
    }
  }

  /** Cierra la planilla que quedó abierta de otro día. */
  async function closeStuck() {
    if (!branch || !stuck) {
      return
    }
    const amount = parseMoney(stuckAmount)
    if (amount == null || amount < 0) {
      setError('Indicá cuánto hay en el cajón.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await closeCashSession(branch.id, stuck.businessDate, amount, '')
      setStuckAmount('')
      setPending(null)
      await reload(branch.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cerrar la caja')
    } finally {
      setBusy(false)
    }
  }

  /** Guarda el recuento de apertura (pisa el monto heredado). */
  async function saveOpeningCount() {
    if (!branch || !session) {
      return
    }
    const amount = parseMoney(countAmount)
    if (amount == null || amount < 0) {
      setError('Indicá cuánto hay en el cajón.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      setSession(await countCashOpening(branch.id, session.businessDate, amount))
      setShowCount(false)
      setCountAmount('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo guardar el recuento')
    } finally {
      setBusy(false)
    }
  }

  /** Anota un retiro, vale, gasto o ingreso. */
  async function addMovement() {
    if (!branch || !session) {
      return
    }
    const amount = parseMoney(moveAmount)
    if (amount == null || amount <= 0) {
      setError('El monto tiene que ser mayor a cero.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await registerCashMovement(branch.id, session.businessDate, moveType, amount, moveDescription)
      setMoveAmount('')
      setMoveDescription('')
      await reload(branch.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo anotar el movimiento')
    } finally {
      setBusy(false)
    }
  }

  /** Abre el formulario para corregir un movimiento. */
  function startEdit(movement: CashMovement) {
    setEditing(movement)
    setEditType(movement.movementType)
    setEditAmount(String(Math.abs(movement.amount)))
    setEditDescription(movement.description ?? '')
    setError(null)
  }

  /** Guarda la corrección del movimiento. */
  async function saveEdit() {
    if (!branch || !editing) {
      return
    }
    const amount = parseMoney(editAmount)
    if (amount == null || amount <= 0) {
      setError('El monto tiene que ser mayor a cero.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await updateCashMovement(branch.id, editing.id, editType, amount, editDescription)
      setEditing(null)
      await reload(branch.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo corregir el movimiento')
    } finally {
      setBusy(false)
    }
  }

  /** Borra el movimiento confirmado. */
  async function removeMovement(movement: CashMovement) {
    if (!branch) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      await deleteCashMovement(branch.id, movement.id)
      setPending(null)
      await reload(branch.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo borrar el movimiento')
    } finally {
      setBusy(false)
    }
  }

  /** Cierra la caja de hoy con lo que hay en el cajón. */
  async function closeToday() {
    if (!branch || !session) {
      return
    }
    const amount = parseMoney(closeAmount)
    if (amount == null || amount < 0) {
      setError('Indicá cuánto hay en el cajón.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      await closeCashSession(branch.id, session.businessDate, amount, closeNote)
      setCloseAmount('')
      setCloseNote('')
      setPending(null)
      await reload(branch.id)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cerrar la caja')
    } finally {
      setBusy(false)
    }
  }

  /** Carga los movimientos de un día anterior. */
  async function toggleHistory(date: string) {
    if (!branch) {
      return
    }
    if (historyDate === date) {
      setHistoryDate(null)
      setHistoryMovements(null)
      return
    }
    setHistoryDate(date)
    try {
      setHistoryMovements(await fetchCashMovements(branch.id, date))
    } catch (err) {
      setHistoryMovements([])
      setError(err instanceof Error ? err.message : 'No se pudo cargar el historial')
    }
  }

  /** Semana anterior del historial. */
  function goToPreviousWeek() {
    setHistoryDate(null)
    setHistoryMovements(null)
    setHistoryFrom(shiftWeek(historyFrom, -1).from)
  }

  /** Semana siguiente, sin pasar de la actual. */
  function goToNextWeek() {
    const next = shiftWeek(historyFrom, 1)
    if (next.from > weekContaining(todayIso()).from) {
      return
    }
    setHistoryDate(null)
    setHistoryMovements(null)
    setHistoryFrom(next.from)
  }

  if (branch == null) {
    return (
      <>
        <div className="page-header">
          <h1>Caja</h1>
        </div>
        <p>Elegí una sucursal en la barra de arriba.</p>
      </>
    )
  }

  const today = todayIso()
  const historyWeek = weekContaining(historyFrom)
  const currentWeek = weekContaining(today)
  const countedPreview = parseMoney(closeAmount)
  const closePreview =
    session && countedPreview != null ? countedPreview - session.expectedAmount : null
  const inherited = previous?.closingAmount ?? 0

  return (
    <>
      <div className="page-header">
        <h1>Caja · {branch.name}</h1>
        <p className="meta">{formatBusinessDate(today)}</p>
      </div>

      {error && <p className="error">{error}</p>}
      {!deskReady && !error && <p>Cargando…</p>}

      {stuck && (
        <form
          className="card form"
          onSubmit={(event) => {
            event.preventDefault()
            setPending({ kind: 'closeStuck' })
          }}
        >
          <h2>Caja anterior sin cerrar</h2>
          <p>
            Quedó abierta la del {formatBusinessDate(stuck.businessDate)} (esperado{' '}
            {formatPrice(stuck.expectedAmount)}). Hay que cerrarla para poder abrir la de hoy.
          </p>
          <label>
            ¿Cuánto hay en el cajón?
            <input
              type="number"
              min={0}
              step="0.01"
              required
              value={stuckAmount}
              onChange={(event) => setStuckAmount(event.target.value)}
            />
          </label>
          <div className="form-actions">
            <button type="submit" disabled={busy}>
              Cerrar caja anterior
            </button>
          </div>
        </form>
      )}

      {!stuck && deskReady && session == null && (
        <section className="card form">
          <h2>La caja de hoy no está abierta</h2>
          <p className="cash-expected">{formatPrice(inherited)}</p>
          <p className="meta">
            {previous == null || previous.closingAmount == null
              ? 'Todavía no hay planilla en este local. Va a arrancar en cero.'
              : 'Eso es lo que quedó del último cierre. El cajón no se vacía: se arrastra.'}
          </p>
          <div className="form-actions">
            <button type="button" disabled={busy} onClick={() => void openToday()}>
              {busy ? 'Abriendo…' : 'Abrir caja'}
            </button>
          </div>
        </section>
      )}

      {session && (
        <>
          <section className="card cash-summary">
            <p className="meta">{session.open ? 'Esperado en el cajón' : 'Caja cerrada · esperado'}</p>
            <p className="cash-expected">{formatPrice(session.expectedAmount)}</p>
            {!session.open && session.difference != null && (
              <p className={session.difference === 0 ? 'pos-flash' : 'error'}>
                Contado {formatPrice(session.closingAmount ?? 0)} · {differenceLabel(session.difference)}
              </p>
            )}
            <dl className="cash-figures">
              <div>
                <dt>Apertura</dt>
                <dd>
                  {formatPrice(session.openingAmount)}
                  <span className="hint">
                    {session.openingCounted ? ' · contada' : ' · heredada, sin contar'}
                  </span>
                </dd>
              </div>
              <div>
                <dt>Ventas en efectivo</dt>
                <dd>{formatPrice(session.totalCashSales)}</dd>
              </div>
              <div>
                <dt>Salidas</dt>
                <dd className="qty-neg">{formatPrice(Math.abs(session.totalCashOut))}</dd>
              </div>
              <div>
                <dt>Ingresos</dt>
                <dd>{formatPrice(session.totalCashIn)}</dd>
              </div>
            </dl>
            {session.note && <p className="meta">{session.note}</p>}
          </section>

          {session.open && (
            <section className="card form">
              <h2>Recuento de apertura</h2>
              {!session.openingCounted && !showCount && (
                <>
                  <p className="meta">
                    El esperado arranca con lo de ayer. Si al contar el cajón da otra cosa, anotalo
                    acá.
                  </p>
                  <div className="form-actions">
                    <button
                      type="button"
                      className="secondary"
                      onClick={() => {
                        setShowCount(true)
                        setCountAmount(String(session.openingAmount))
                      }}
                    >
                      Contar el cajón
                    </button>
                  </div>
                </>
              )}
              {session.openingCounted && !showCount && (
                <div className="form-actions">
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => {
                      setShowCount(true)
                      setCountAmount(String(session.openingAmount))
                    }}
                  >
                    Volver a contar
                  </button>
                </div>
              )}
              {showCount && (
                <>
                  <label>
                    ¿Cuánto hay en el cajón?
                    <input
                      type="number"
                      min={0}
                      step="0.01"
                      required
                      value={countAmount}
                      onChange={(event) => setCountAmount(event.target.value)}
                    />
                  </label>
                  <div className="form-actions">
                    <button type="button" disabled={busy} onClick={() => void saveOpeningCount()}>
                      Guardar recuento
                    </button>
                    <button type="button" className="secondary" onClick={() => setShowCount(false)}>
                      Cancelar
                    </button>
                  </div>
                </>
              )}
            </section>
          )}

          {session.open && (
            <form
              className="card form"
              onSubmit={(event) => {
                event.preventDefault()
                void addMovement()
              }}
            >
              <h2>Anotar movimiento</h2>
              <div className="choice-row">
                {CASH_MOVEMENT_TYPES.map((type) => (
                  <button
                    key={type}
                    type="button"
                    className={moveType === type ? 'small' : 'small secondary'}
                    onClick={() => setMoveType(type)}
                  >
                    {cashMovementLabel(type)}
                  </button>
                ))}
              </div>
              <label>
                Monto
                <input
                  type="number"
                  min={0.01}
                  step="0.01"
                  required
                  value={moveAmount}
                  onChange={(event) => setMoveAmount(event.target.value)}
                />
              </label>
              <label>
                Nota
                <input
                  value={moveDescription}
                  placeholder={
                    moveType === 'VALE'
                      ? 'vale Juan'
                      : moveType === 'GASTO'
                        ? 'proveedor, taxi, etc.'
                        : 'opcional'
                  }
                  onChange={(event) => setMoveDescription(event.target.value)}
                />
              </label>
              <div className="form-actions">
                <button type="submit" disabled={busy}>
                  Anotar {cashMovementLabel(moveType).toLowerCase()}
                </button>
              </div>
            </form>
          )}

          <section className="card">
            <h2>Movimientos</h2>
            {movements.length === 0 && (
              <p className="meta">Todavía no hay retiros, vales, gastos ni ingresos.</p>
            )}
            <ul className="cash-movements">
              {movements.map((movement) => (
                <li key={movement.id}>
                  {editing?.id === movement.id ? (
                    <form
                      className="form cash-edit"
                      onSubmit={(event) => {
                        event.preventDefault()
                        void saveEdit()
                      }}
                    >
                      <div className="choice-row">
                        {CASH_MOVEMENT_TYPES.map((type) => (
                          <button
                            key={type}
                            type="button"
                            className={editType === type ? 'small' : 'small secondary'}
                            onClick={() => setEditType(type)}
                          >
                            {cashMovementLabel(type)}
                          </button>
                        ))}
                      </div>
                      <label>
                        Monto
                        <input
                          type="number"
                          min={0.01}
                          step="0.01"
                          required
                          value={editAmount}
                          onChange={(event) => setEditAmount(event.target.value)}
                        />
                      </label>
                      <label>
                        Nota
                        <input
                          value={editDescription}
                          onChange={(event) => setEditDescription(event.target.value)}
                        />
                      </label>
                      <div className="form-actions">
                        <button type="submit" disabled={busy}>
                          Guardar
                        </button>
                        <button type="button" className="secondary" onClick={() => setEditing(null)}>
                          Cancelar
                        </button>
                      </div>
                    </form>
                  ) : (
                    <>
                      <div>
                        <strong>{cashMovementLabel(movement.movementType)}</strong>
                        {movement.description ? ` · ${movement.description}` : ''}
                        <p className="meta">{formatMovementAt(movement.movementAt)}</p>
                      </div>
                      <span className={movement.amount < 0 ? 'qty-neg' : 'qty'}>
                        {formatPrice(movement.amount)}
                      </span>
                      {session.open && (
                        <div className="cash-row-actions">
                          <button
                            type="button"
                            className="small secondary"
                            onClick={() => startEdit(movement)}
                          >
                            Editar
                          </button>
                          <button
                            type="button"
                            className="small danger"
                            onClick={() => setPending({ kind: 'delete', movement })}
                          >
                            Borrar
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </li>
              ))}
            </ul>
          </section>

          {session.open && (
            <form
              className="card form"
              onSubmit={(event) => {
                event.preventDefault()
                setPending({ kind: 'close' })
              }}
            >
              <h2>Cerrar caja</h2>
              <p className="meta">Esperado {formatPrice(session.expectedAmount)}.</p>
              <label>
                ¿Cuánto hay en el cajón?
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  required
                  value={closeAmount}
                  onChange={(event) => setCloseAmount(event.target.value)}
                />
              </label>
              {closePreview != null && (
                <p className={closePreview === 0 ? 'pos-flash' : 'meta'}>{differenceLabel(closePreview)}</p>
              )}
              <label>
                Nota
                <input
                  value={closeNote}
                  placeholder="opcional"
                  onChange={(event) => setCloseNote(event.target.value)}
                />
              </label>
              <div className="form-actions">
                <button type="submit" disabled={busy}>
                  Cerrar caja
                </button>
              </div>
            </form>
          )}
        </>
      )}

      {deskReady && (
        <section className="list cash-history">
          <div className="cash-history-nav">
            <h2>Historial</h2>
            <div className="page-actions">
              <button type="button" className="small secondary" onClick={goToPreviousWeek}>
                Semana anterior
              </button>
              <span className="meta">{formatWeekRange(historyWeek.from, historyWeek.to)}</span>
              <button
                type="button"
                className="small secondary"
                disabled={historyWeek.from >= currentWeek.from}
                onClick={goToNextWeek}
              >
                Semana siguiente
              </button>
            </div>
          </div>
          {history === null && <p className="meta">Cargando…</p>}
          {history && history.length === 0 && (
            <p className="meta">No hay planillas del {formatWeekRange(historyWeek.from, historyWeek.to)}.</p>
          )}
          {history &&
            history.map((item) => (
              <article key={item.id} className="card">
                <header className="card-header">
                  <h2>{formatBusinessDate(item.businessDate)}</h2>
                  <span className={item.open ? 'error' : 'qty'}>
                    {item.open ? 'Abierta' : formatPrice(item.expectedAmount)}
                  </span>
                </header>
                <p className="meta">
                  {item.open
                    ? `Esperado ${formatPrice(item.expectedAmount)}`
                    : `Contado ${formatPrice(item.closingAmount ?? 0)}${
                        item.difference != null ? ` · ${differenceLabel(item.difference)}` : ''
                      }`}
                </p>
                <div className="form-actions">
                  <button
                    type="button"
                    className="small secondary"
                    onClick={() => void toggleHistory(item.businessDate)}
                  >
                    {historyDate === item.businessDate ? 'Ocultar movimientos' : 'Ver movimientos'}
                  </button>
                </div>
                {historyDate === item.businessDate && (
                  <ul className="cash-movements">
                    {(historyMovements ?? []).length === 0 && (
                      <li>
                        <span className="meta">No hubo retiros ni ingresos.</span>
                      </li>
                    )}
                    {(historyMovements ?? []).map((movement) => (
                      <li key={movement.id}>
                        <div>
                          <strong>{cashMovementLabel(movement.movementType)}</strong>
                          {movement.description ? ` · ${movement.description}` : ''}
                          <p className="meta">{formatMovementAt(movement.movementAt)}</p>
                        </div>
                        <span className={movement.amount < 0 ? 'qty-neg' : 'qty'}>
                          {formatPrice(movement.amount)}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </article>
            ))}
        </section>
      )}

      {pending?.kind === 'close' && (
        <DoubleConfirm
          title="Cerrar caja"
          firstMessage="Si cerrás, no se puede volver a anotar movimientos en este día. ¿Estás seguro?"
          secondMessage="¿Realmente estás seguro? Una caja cerrada no se reabre."
          confirmDanger
          onConfirm={() => void closeToday()}
          onCancel={() => setPending(null)}
        />
      )}
      {pending?.kind === 'closeStuck' && stuck && (
        <DoubleConfirm
          title="Cerrar caja anterior"
          firstMessage={`Vas a cerrar la del ${formatBusinessDate(stuck.businessDate)}. ¿Estás seguro?`}
          secondMessage="¿Realmente estás seguro? Esa planilla no se reabre."
          confirmDanger
          onConfirm={() => void closeStuck()}
          onCancel={() => setPending(null)}
        />
      )}
      {pending?.kind === 'delete' && (
        <DoubleConfirm
          title="Borrar movimiento"
          firstMessage={`Vas a borrar el ${cashMovementLabel(pending.movement.movementType).toLowerCase()} de ${formatPrice(Math.abs(pending.movement.amount))}. ¿Estás seguro?`}
          secondMessage="¿Realmente estás seguro?"
          confirmDanger
          onConfirm={() => void removeMovement(pending.movement)}
          onCancel={() => setPending(null)}
        />
      )}
    </>
  )
}
