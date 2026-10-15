import { useId, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import { ApiError, createAnalysis } from '../api/client'
import { checkUrl, MAX_URL_LENGTH } from '../api/url'

export function UrlForm() {
  const inputId = useId()
  const errorId = useId()
  const hintId = useId()
  const navigate = useNavigate()
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const check = checkUrl(value)
    if (!check.ok) {
      setError(check.message)
      return
    }
    setError(null)
    setSending(true)
    try {
      const created = await createAnalysis(check.url)
      await navigate(`/analisis/${created.id}`)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Se produjo un error inesperado.')
      setSending(false)
    }
  }

  return (
    <form className="url-form" onSubmit={onSubmit} noValidate>
      <label htmlFor={inputId}>Dirección de tu web</label>
      <p id={hintId} className="hint">
        Por ejemplo: minegocio.es o https://www.minegocio.es/servicios
      </p>
      <div className="url-form-row">
        <input
          id={inputId}
          name="url"
          type="text"
          inputMode="url"
          autoComplete="url"
          spellCheck={false}
          maxLength={MAX_URL_LENGTH}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          aria-invalid={error !== null}
          aria-describedby={error ? `${hintId} ${errorId}` : hintId}
          disabled={sending}
        />
        <button type="submit" disabled={sending}>
          {sending ? 'Enviando…' : 'Analizar'}
        </button>
      </div>
      {error && (
        <p id={errorId} className="form-error" role="alert">
          {error}
        </p>
      )}
    </form>
  )
}
