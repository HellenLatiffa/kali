import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import './App.css'

type Profile = {
  id: string
  email: string
  displayName: string
}

type ConsentRecord = {
  purpose: string
  policyVersion: string
  grantedAt: string
  revokedAt: string | null
}

type ConsentStatus = {
  purpose: string
  policyVersion: string
  status: 'REQUIRED' | 'GRANTED' | 'REVOKED' | 'RECONSENT_REQUIRED'
  notice: string
  history: ConsentRecord[]
}

type PeriodEntry = {
  id: string
  entryDate: string
  bleedingType: 'PERIOD' | 'SPOTTING'
  flowLevel: 'LIGHT' | 'MEDIUM' | 'HEAVY'
  cycleStart: boolean
}

type CycleEstimate =
  | {
      estimateAvailable: true
      recordedCycleStarts: number
      intervalsUsed: number
      lastCycleStart: string
      estimatedNextPeriod: string
      earliestPossibleDate: string
      latestPossibleDate: string
      medianCycleDays: number
      shortestObservedCycleDays: number
      longestObservedCycleDays: number
    }
  | {
      estimateAvailable: false
      recordedCycleStarts: number
      intervalsUsed: number
      lastCycleStart: null
      estimatedNextPeriod: null
      earliestPossibleDate: null
      latestPossibleDate: null
      medianCycleDays: null
      shortestObservedCycleDays: null
      longestObservedCycleDays: null
    }

type SymptomType =
  | 'CRAMPS'
  | 'PAIN'
  | 'HEADACHE'
  | 'BLOATING'
  | 'ACNE'
  | 'BREAST_TENDERNESS'
  | 'BOWEL_CHANGES'
  | 'ENERGY_CHANGE'
  | 'APPETITE_CHANGE'
  | 'SLEEP_CHANGE'
  | 'DISCHARGE'

type SymptomEntry = {
  id: string
  entryDate: string
  symptomType: SymptomType
  intensity: number
  notes: string | null
}

type MoodLevel = 'VERY_LOW' | 'LOW' | 'NEUTRAL' | 'GOOD' | 'VERY_GOOD'

type WellbeingEntry = {
  id: string
  entryDate: string
  mood: MoodLevel
  stressLevel: number | null
  energyLevel: number | null
  sleepQuality: number | null
  reflection: string | null
}

type RegistrationResponse = {
  message: string
}

type ExportData = {
  exportedAt: string
  account: {
    id: string
    email: string
    displayName: string
    createdAt: string
  }
  consents: ConsentRecord[]
  periodEntries: PeriodEntry[]
  symptomEntries: SymptomEntry[]
  wellbeingEntries: WellbeingEntry[]
}

type ApiError = {
  code?: string
  message?: string
}

class ApiRequestError extends Error {
  readonly code?: string

  constructor(message: string, code?: string) {
    super(message)
    this.code = code
  }
}

const csrfCookieName = 'XSRF-TOKEN'

const symptomOptions: { value: SymptomType; label: string }[] = [
  { value: 'CRAMPS', label: 'Cólicas' },
  { value: 'PAIN', label: 'Dor' },
  { value: 'HEADACHE', label: 'Dor de cabeça' },
  { value: 'BLOATING', label: 'Inchaço' },
  { value: 'ACNE', label: 'Acne' },
  { value: 'BREAST_TENDERNESS', label: 'Sensibilidade mamária' },
  { value: 'BOWEL_CHANGES', label: 'Alterações intestinais' },
  { value: 'ENERGY_CHANGE', label: 'Mudança na energia' },
  { value: 'APPETITE_CHANGE', label: 'Mudança no apetite' },
  { value: 'SLEEP_CHANGE', label: 'Mudança no sono' },
  { value: 'DISCHARGE', label: 'Secreção' },
]

const moodOptions: { value: MoodLevel; label: string }[] = [
  { value: 'VERY_LOW', label: 'Muito baixo' },
  { value: 'LOW', label: 'Baixo' },
  { value: 'NEUTRAL', label: 'Neutro' },
  { value: 'GOOD', label: 'Bom' },
  { value: 'VERY_GOOD', label: 'Muito bom' },
]

function readCookie(name: string) {
  const cookie = document.cookie
    .split('; ')
    .find((part) => part.startsWith(`${name}=`))

  return cookie ? decodeURIComponent(cookie.slice(name.length + 1)) : ''
}

async function ensureCsrf() {
  const response = await fetch('/api/v1/auth/csrf', {
    credentials: 'same-origin',
  })
  if (!response.ok) {
    throw new Error('Não foi possível preparar uma solicitação segura.')
  }
}

async function request<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase()
  const headers = new Headers(init.headers)

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    await ensureCsrf()
    const token = readCookie(csrfCookieName)
    if (!token) {
      throw new Error('Não foi possível preparar uma solicitação segura.')
    }
    headers.set('X-XSRF-TOKEN', token)
  }

  const response = await fetch(path, {
    ...init,
    method,
    headers,
    credentials: 'same-origin',
  })

  if (!response.ok) {
    const error = (await response.json().catch(() => ({}))) as ApiError
    throw new ApiRequestError(
      error.message ?? 'A solicitação não pôde ser concluída.',
      error.code,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}

function currentMonthKey() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`
}

function todayDateKey() {
  const today = new Date()
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}

function monthBounds(monthKey: string) {
  const [yearText, monthText] = monthKey.split('-')
  const year = Number(yearText)
  const month = Number(monthText)
  const lastDay = new Date(year, month, 0).getDate()
  const monthPart = String(month).padStart(2, '0')
  return {
    from: `${yearText}-${monthPart}-01`,
    to: `${yearText}-${monthPart}-${String(lastDay).padStart(2, '0')}`,
    days: lastDay,
    firstWeekday: (new Date(year, month - 1, 1).getDay() + 6) % 7,
  }
}

function formatPeriodDate(date: string) {
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' })
    .format(new Date(`${date}T12:00:00`))
}

function App() {
  const [profile, setProfile] = useState<Profile | null>(null)
  const [consent, setConsent] = useState<ConsentStatus | null>(null)
  const [periodEntries, setPeriodEntries] = useState<PeriodEntry[]>([])
  const [cycleEstimate, setCycleEstimate] = useState<CycleEstimate | null>(null)
  const [symptomEntries, setSymptomEntries] = useState<SymptomEntry[]>([])
  const [wellbeingEntries, setWellbeingEntries] = useState<WellbeingEntry[]>([])
  const [periodMonth, setPeriodMonth] = useState(currentMonthKey)
  const [editingPeriod, setEditingPeriod] = useState<PeriodEntry | null>(null)
  const [editingSymptom, setEditingSymptom] = useState<SymptomEntry | null>(null)
  const [editingWellbeing, setEditingWellbeing] = useState<WellbeingEntry | null>(null)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  useEffect(() => {
    if (!profile || consent?.status !== 'GRANTED') {
      return
    }

    let active = true
    const range = monthBounds(periodMonth)
    void Promise.all([
      request<PeriodEntry[]>(`/api/v1/periods?from=${range.from}&to=${range.to}`),
      request<SymptomEntry[]>(`/api/v1/symptom-entries?from=${range.from}&to=${range.to}`),
      request<WellbeingEntry[]>(`/api/v1/mood-entries?from=${range.from}&to=${range.to}`),
      request<CycleEstimate>('/api/v1/insights/dashboard'),
    ])
      .then(([periods, symptoms, wellbeingRecords, insights]) => {
        if (active) {
          setPeriodEntries(periods)
          setSymptomEntries(symptoms)
          setWellbeingEntries(wellbeingRecords)
          setCycleEstimate(insights)
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(reason instanceof Error ? reason.message : 'Não foi possível carregar o calendário.')
        }
      })
    return () => {
      active = false
    }
  }, [profile, consent?.status, periodMonth])

  // This effect hydrates the UI from the existing server-side session after network requests.
  useEffect(() => {
    let active = true
    async function hydrateSession() {
      try {
        const currentProfile = await request<Profile>('/api/v1/me')
        const currentConsent = await request<ConsentStatus>(
          '/api/v1/privacy/consents/health-data',
        )
        if (active) {
          setProfile(currentProfile)
          setConsent(currentConsent)
        }
      } catch (reason) {
        if (active) {
          if (!(reason instanceof ApiRequestError) || reason.code !== 'AUTHENTICATION_REQUIRED') {
            setError(
              reason instanceof Error ? reason.message : 'Não foi possível verificar a sessão.',
            )
          }
          setProfile(null)
          setConsent(null)
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    void hydrateSession()
    return () => {
      active = false
    }
  }, [])

  async function submitAuth(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    const form = new FormData(event.currentTarget)
    const formElement = event.currentTarget
    const body = {
      email: String(form.get('email') ?? ''),
      password: String(form.get('password') ?? ''),
      ...(mode === 'register'
        ? { displayName: String(form.get('displayName') ?? '') }
        : {}),
    }

    try {
      if (mode === 'register') {
        const result = await request<RegistrationResponse>(
          '/api/v1/auth/register',
          { method: 'POST', body: JSON.stringify(body) },
        )
        setMode('login')
        setNotice(result.message)
        formElement.reset()
      } else {
        const currentProfile = await request<Profile>('/api/v1/auth/login', {
          method: 'POST',
          body: JSON.stringify(body),
        })
        setProfile(currentProfile)
        const currentConsent = await request<ConsentStatus>(
          '/api/v1/privacy/consents/health-data',
        )
        setConsent(currentConsent)
        formElement.reset()
      }
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : 'Não foi possível continuar.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function updateProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    const form = new FormData(event.currentTarget)

    try {
      const updated = await request<Profile>('/api/v1/me/profile', {
        method: 'PUT',
        body: JSON.stringify({
          displayName: String(form.get('displayName') ?? ''),
        }),
      })
      setProfile(updated)
      setNotice('Perfil atualizado.')
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : 'Não foi possível salvar.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function updateConsent(grant: boolean) {
    setBusy(true)
    setError('')
    setNotice('')

    try {
      const updated = await request<ConsentStatus>(
        '/api/v1/privacy/consents/health-data',
        {
          method: grant ? 'PUT' : 'DELETE',
          ...(grant
            ? { body: JSON.stringify({ accepted: true }) }
            : {}),
        },
      )
      setConsent(updated)
      if (!grant) {
        setPeriodEntries([])
        setCycleEstimate(null)
        setSymptomEntries([])
        setWellbeingEntries([])
        setEditingPeriod(null)
        setEditingSymptom(null)
        setEditingWellbeing(null)
      }
      setNotice(
        grant
          ? 'Consentimento registrado. Você pode revogá-lo a qualquer momento.'
          : 'Consentimento revogado. O processamento de dados de saúde está bloqueado.',
      )
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : 'Não foi possível atualizar.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function savePeriodEntry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const body = JSON.stringify({
      entryDate: String(form.get('entryDate') ?? ''),
      bleedingType: String(form.get('bleedingType') ?? ''),
      flowLevel: String(form.get('flowLevel') ?? ''),
      cycleStart: form.get('cycleStart') === 'on',
    })

    try {
      const saved = await request<PeriodEntry>(
        editingPeriod
          ? `/api/v1/periods/${editingPeriod.id}`
          : '/api/v1/periods',
        {
          method: editingPeriod ? 'PUT' : 'POST',
          body,
        },
      )
      setEditingPeriod(null)
      formElement.reset()
      const savedMonth = saved.entryDate.slice(0, 7)
      if (savedMonth !== periodMonth) {
        setPeriodMonth(savedMonth)
      } else {
        const range = monthBounds(periodMonth)
        const updated = await request<PeriodEntry[]>(
          `/api/v1/periods?from=${range.from}&to=${range.to}`,
        )
        setPeriodEntries(updated)
      }
      setCycleEstimate(await request<CycleEstimate>('/api/v1/insights/dashboard'))
      setNotice('Registro do calendário salvo.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível salvar o registro.')
    } finally {
      setBusy(false)
    }
  }

  async function saveSymptomEntry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const body = JSON.stringify({
      entryDate: String(form.get('symptomDate') ?? ''),
      symptomType: String(form.get('symptomType') ?? ''),
      intensity: Number(form.get('intensity')),
      notes: String(form.get('symptomNotes') ?? '') || null,
    })

    try {
      const saved = await request<SymptomEntry>(
        editingSymptom
          ? `/api/v1/symptom-entries/${editingSymptom.id}`
          : '/api/v1/symptom-entries',
        {
          method: editingSymptom ? 'PUT' : 'POST',
          body,
        },
      )
      setEditingSymptom(null)
      formElement.reset()
      const savedMonth = saved.entryDate.slice(0, 7)
      if (savedMonth !== periodMonth) {
        setPeriodMonth(savedMonth)
      } else {
        const range = monthBounds(periodMonth)
        setSymptomEntries(await request<SymptomEntry[]>(
          `/api/v1/symptom-entries?from=${range.from}&to=${range.to}`,
        ))
      }
      setNotice('Registro de sintoma salvo.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível salvar o registro.')
    } finally {
      setBusy(false)
    }
  }

  async function deleteSymptomEntry(entry: SymptomEntry) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await request<void>(`/api/v1/symptom-entries/${entry.id}`, { method: 'DELETE' })
      setSymptomEntries((current) => current.filter((item) => item.id !== entry.id))
      setEditingSymptom(null)
      setNotice('Registro de sintoma removido.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível remover o registro.')
    } finally {
      setBusy(false)
    }
  }

  async function saveWellbeingEntry(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    const formElement = event.currentTarget
    const form = new FormData(formElement)
    const optionalLevel = (name: string) => {
      const value = String(form.get(name) ?? '')
      return value ? Number(value) : null
    }
    const body = JSON.stringify({
      entryDate: String(form.get('wellbeingDate') ?? ''),
      mood: String(form.get('mood') ?? ''),
      stressLevel: optionalLevel('stressLevel'),
      energyLevel: optionalLevel('energyLevel'),
      sleepQuality: optionalLevel('sleepQuality'),
      reflection: String(form.get('reflection') ?? '') || null,
    })

    try {
      const saved = await request<WellbeingEntry>(
        editingWellbeing
          ? `/api/v1/mood-entries/${editingWellbeing.id}`
          : '/api/v1/mood-entries',
        {
          method: editingWellbeing ? 'PUT' : 'POST',
          body,
        },
      )
      setEditingWellbeing(null)
      formElement.reset()
      const savedMonth = saved.entryDate.slice(0, 7)
      if (savedMonth !== periodMonth) {
        setPeriodMonth(savedMonth)
      } else {
        const range = monthBounds(periodMonth)
        setWellbeingEntries(await request<WellbeingEntry[]>(
          `/api/v1/mood-entries?from=${range.from}&to=${range.to}`,
        ))
      }
      setNotice('Registro de bem-estar salvo.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível salvar o registro.')
    } finally {
      setBusy(false)
    }
  }

  async function deleteWellbeingEntry(entry: WellbeingEntry) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await request<void>(`/api/v1/mood-entries/${entry.id}`, { method: 'DELETE' })
      setWellbeingEntries((current) => current.filter((item) => item.id !== entry.id))
      setEditingWellbeing(null)
      setNotice('Registro de bem-estar removido.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível remover o registro.')
    } finally {
      setBusy(false)
    }
  }

  async function deletePeriodEntry(entry: PeriodEntry) {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await request<void>(`/api/v1/periods/${entry.id}`, { method: 'DELETE' })
      setPeriodEntries((current) => current.filter((item) => item.id !== entry.id))
      setCycleEstimate(await request<CycleEstimate>('/api/v1/insights/dashboard'))
      setEditingPeriod(null)
      setNotice('Registro removido.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível remover o registro.')
    } finally {
      setBusy(false)
    }
  }

  async function exportData(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setBusy(true)
    setError('')
    setNotice('')
    const form = new FormData(event.currentTarget)
    const formElement = event.currentTarget

    try {
      const data = await request<ExportData>('/api/v1/me/export', {
        method: 'POST',
        body: JSON.stringify({ password: String(form.get('password') ?? '') }),
      })
      const file = new Blob([JSON.stringify(data, null, 2)], {
        type: 'application/json',
      })
      const url = URL.createObjectURL(file)
      const link = document.createElement('a')
      link.href = url
      link.download = `kali-export-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      URL.revokeObjectURL(url)
      formElement.reset()
      setNotice('Exportação gerada para este dispositivo.')
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : 'Não foi possível exportar.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function deleteAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    if (form.get('confirmed') !== 'on') {
      setError('Confirme que deseja excluir a conta e os dados associados.')
      return
    }

    setBusy(true)
    setError('')
    setNotice('')
    try {
      await request<void>('/api/v1/me', {
        method: 'DELETE',
        body: JSON.stringify({ password: String(form.get('password') ?? '') }),
      })
      setProfile(null)
      setConsent(null)
      setPeriodEntries([])
      setCycleEstimate(null)
      setSymptomEntries([])
      setWellbeingEntries([])
      setEditingPeriod(null)
      setEditingSymptom(null)
      setEditingWellbeing(null)
      setMode('login')
      setNotice('Conta e dados associados foram excluídos.')
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : 'Não foi possível excluir.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function logout() {
    setBusy(true)
    setError('')
    try {
      await request<void>('/api/v1/auth/logout', { method: 'POST' })
      setProfile(null)
      setConsent(null)
      setPeriodEntries([])
      setCycleEstimate(null)
      setSymptomEntries([])
      setWellbeingEntries([])
      setEditingPeriod(null)
      setEditingSymptom(null)
      setEditingWellbeing(null)
      setMode('login')
      setNotice('Sessão encerrada.')
    } catch (reason) {
      setError(
        reason instanceof Error ? reason.message : 'Não foi possível sair.',
      )
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <a className="brand" href="/" aria-label="Kali, início">
          <span className="brand-mark" aria-hidden="true">k</span>
          <span>Kali</span>
        </a>
        {profile ? (
          <button className="text-button" type="button" onClick={logout} disabled={busy}>
            Sair
          </button>
        ) : (
          <span className="environment-label">Privacidade por padrão</span>
        )}
      </header>

      <section className="content" aria-labelledby="page-title">
        <p className="eyebrow">Identidade e privacidade</p>
        <h1 id="page-title">
          {profile ? `Olá, ${profile.displayName}.` : 'Seu espaço, no seu tempo.'}
        </h1>
        <p className="intro">
          Registre os dias observados do seu ciclo e controle seus dados com clareza.
          Com histórico suficiente, veja uma estimativa pessoal e incerta do próximo início menstrual.
        </p>

        {error && <p className="feedback error" role="alert">{error}</p>}
        {notice && <p className="feedback success" role="status">{notice}</p>}

        {loading ? (
          <p role="status">Verificando sua sessão…</p>
        ) : profile && consent ? (
          <div className="account-grid">
            <section className="panel" aria-labelledby="profile-heading">
              <h2 id="profile-heading">Seu perfil</h2>
              <p className="muted">{profile.email}</p>
              <form className="form-stack" onSubmit={updateProfile}>
                <label htmlFor="profile-name">Como gostaria de ser chamada?</label>
                <input
                  id="profile-name"
                  name="displayName"
                  autoComplete="nickname"
                  maxLength={80}
                  defaultValue={profile.displayName}
                  required
                />
                <button className="primary-button" type="submit" disabled={busy}>
                  Salvar perfil
                </button>
              </form>
            </section>

            <section className="panel consent-panel" aria-labelledby="consent-heading">
              <div className="section-heading">
                <div>
                  <h2 id="consent-heading">Dados de saúde</h2>
                  <p className="muted">Finalidade: {consent.purpose}</p>
                </div>
                <span className={`status-pill status-${consent.status.toLowerCase()}`}>
                  {consent.status === 'GRANTED' ? 'Autorizado' : 'Não autorizado'}
                </span>
              </div>
              <p>{consent.notice}</p>
              <p className="muted">Versão do aviso: {consent.policyVersion}</p>
              {consent.status === 'GRANTED' ? (
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() => updateConsent(false)}
                  disabled={busy}
                >
                  Revogar consentimento
                </button>
              ) : (
                <form
                  className="consent-form"
                  onSubmit={(event) => {
                    event.preventDefault()
                    if (new FormData(event.currentTarget).get('accepted') === 'on') {
                      void updateConsent(true)
                    }
                  }}
                >
                  <label className="checkbox-label">
                    <input type="checkbox" name="accepted" required />
                    <span>Li o aviso e autorizo este tratamento específico.</span>
                  </label>
                  <button className="primary-button" type="submit" disabled={busy}>
                    Registrar consentimento
                  </button>
                </form>
              )}
              <p className="notice-box">
                {consent.status === 'GRANTED'
                  ? 'Seu consentimento está ativo. A revogação bloqueia novos processamentos de saúde; perfil e direitos de acesso permanecem disponíveis.'
                  : 'Sem consentimento vigente, operações com dados de saúde ficam bloqueadas. Seu perfil, exportação e exclusão continuam disponíveis.'}
              </p>
              {consent.history.length > 0 && (
                <details>
                  <summary>Histórico de consentimentos</summary>
                  <ul className="history-list">
                    {consent.history.map((record) => (
                      <li key={`${record.purpose}-${record.policyVersion}-${record.grantedAt}`}>
                        Versão {record.policyVersion}: concedido em{' '}
                        {new Date(record.grantedAt).toLocaleString('pt-BR')}
                        {record.revokedAt &&
                          ` · revogado em ${new Date(record.revokedAt).toLocaleString('pt-BR')}`}
                      </li>
                    ))}
                  </ul>
                </details>
              )}
            </section>

            {consent.status === 'GRANTED' && (
              <section className="panel" aria-labelledby="dashboard-heading">
                <h2 id="dashboard-heading">Resumo do ciclo</h2>
                <p className="muted">
                  Visão pessoal baseada somente nos inícios de menstruação que você marcou.
                </p>
                {cycleEstimate ? (
                  cycleEstimate.estimateAvailable ? (
                    <>
                      <div className="insight-grid" aria-label="Estimativa e estatísticas do ciclo">
                        <article className="insight-card">
                          <h3>Próximo início estimado</h3>
                          <p className="insight-value">
                            {formatPeriodDate(cycleEstimate.estimatedNextPeriod)}
                          </p>
                          <p className="muted">
                            Faixa baseada no histórico: {formatPeriodDate(cycleEstimate.earliestPossibleDate)}
                            {' '}a {formatPeriodDate(cycleEstimate.latestPossibleDate)}.
                          </p>
                        </article>
                        <article className="insight-card">
                          <h3>Variação observada</h3>
                          <p className="insight-value">
                            {cycleEstimate.shortestObservedCycleDays}–{cycleEstimate.longestObservedCycleDays} dias
                          </p>
                          <p className="muted">
                            Mediana: {cycleEstimate.medianCycleDays} dias · {cycleEstimate.intervalsUsed} intervalos utilizados.
                          </p>
                        </article>
                      </div>
                      <p className="notice-box" role="note">
                        Esta é uma estimativa incerta calculada com até 6 intervalos recentes. A faixa mostra a variação observada, não uma garantia nem intervalo clínico. Não prevê ovulação ou fertilidade e não é método contraceptivo nem diagnóstico.
                      </p>
                    </>
                  ) : (
                    <p className="notice-box" role="status">
                      Marque o primeiro dia de pelo menos 4 menstruações para mostrar uma estimativa baseada em 3 ou mais intervalos.
                      {' '}Até agora: {cycleEstimate.recordedCycleStarts} de 4 inícios.
                    </p>
                  )
                ) : (
                  <p className="muted" role="status">Carregando o resumo do ciclo…</p>
                )}
              </section>
            )}

            <section className="panel" aria-labelledby="period-heading">
              <div className="section-heading">
                <div>
                  <h2 id="period-heading">Calendário menstrual</h2>
                  <p className="muted">
                    Registre observações diárias. Para habilitar a estimativa, marque explicitamente o primeiro dia de cada menstruação.
                  </p>
                </div>
                {consent.status === 'GRANTED' && (
                  <label className="month-picker">
                    <span>Mês</span>
                    <input
                      type="month"
                      value={periodMonth}
                      onChange={(event) => {
                        if (event.currentTarget.value) {
                          setPeriodMonth(event.currentTarget.value)
                          setEditingPeriod(null)
                          setEditingSymptom(null)
                          setEditingWellbeing(null)
                        }
                      }}
                      aria-label="Mês do calendário"
                    />
                  </label>
                )}
              </div>

              {consent.status !== 'GRANTED' ? (
                <p className="notice-box">
                  Conceda o consentimento informado acima para habilitar o registro de dados de saúde.
                </p>
              ) : (
                <>
                  <div className="calendar-grid" role="group" aria-label="Dias registrados no mês">
                    {['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'].map((day) => (
                      <span className="calendar-weekday" aria-hidden="true" key={day}>
                        {day}
                      </span>
                    ))}
                    {Array.from(
                      { length: monthBounds(periodMonth).firstWeekday },
                      (_, index) => (
                        <span className="calendar-day empty" aria-hidden="true" key={`empty-${index}`} />
                      ),
                    )}
                    {Array.from({ length: monthBounds(periodMonth).days }, (_, index) => {
                      const day = index + 1
                      const date = `${periodMonth}-${String(day).padStart(2, '0')}`
                      const entry = periodEntries.find((item) => item.entryDate === date)
                      const dateLabel = formatPeriodDate(date)
                      return (
                        <div
                          className={`calendar-day${entry ? ' has-entry' : ''}`}
                          role="group"
                          aria-label={entry ? `${dateLabel}: ${entry.bleedingType === 'PERIOD' ? 'menstruação' : 'escape'}, fluxo ${entry.flowLevel.toLowerCase()}${entry.cycleStart ? ', início do ciclo marcado' : ''}` : dateLabel}
                          key={date}
                        >
                          <span className="calendar-date">{day}</span>
                          {entry && (
                            <>
                              <span className="calendar-entry">
                                {entry.bleedingType === 'PERIOD' ? 'Menstruação' : 'Escape'}
                              </span>
                              <span className="calendar-flow">
                                Fluxo {entry.flowLevel === 'LIGHT' ? 'leve' : entry.flowLevel === 'MEDIUM' ? 'moderado' : 'intenso'}
                              </span>
                              {entry.cycleStart && <span className="calendar-cycle-start">Início do ciclo</span>}
                              <button
                                className="text-button calendar-edit"
                                type="button"
                                onClick={() => setEditingPeriod(entry)}
                                disabled={busy}
                                aria-label={`Editar registro de ${dateLabel}`}
                              >
                                Editar
                              </button>
                            </>
                          )}
                        </div>
                      )
                    })}
                  </div>

                  <form
                    className="form-stack period-form"
                    onSubmit={savePeriodEntry}
                    key={editingPeriod?.id ?? 'new-period-entry'}
                  >
                    <h3>{editingPeriod ? 'Editar observação' : 'Registrar observação'}</h3>
                    <label htmlFor="period-date">Data observada</label>
                    <input
                      id="period-date"
                      name="entryDate"
                      type="date"
                      max={todayDateKey()}
                      defaultValue={editingPeriod?.entryDate ?? todayDateKey()}
                      required
                    />
                    <label htmlFor="bleeding-type">Tipo</label>
                    <select
                      id="bleeding-type"
                      name="bleedingType"
                      defaultValue={editingPeriod?.bleedingType ?? 'PERIOD'}
                      onChange={(event) => {
                        const checkbox = event.currentTarget.form
                          ?.querySelector<HTMLInputElement>('#period-cycle-start')
                        if (checkbox) {
                          checkbox.disabled = event.currentTarget.value === 'SPOTTING'
                          if (checkbox.disabled) {
                            checkbox.checked = false
                          }
                        }
                      }}
                    >
                      <option value="PERIOD">Menstruação</option>
                      <option value="SPOTTING">Escape</option>
                    </select>
                    <label className="checkbox-label" htmlFor="period-cycle-start">
                      <input
                        id="period-cycle-start"
                        name="cycleStart"
                        type="checkbox"
                        defaultChecked={editingPeriod?.cycleStart ?? false}
                        disabled={editingPeriod?.bleedingType === 'SPOTTING'}
                      />
                      Este é o primeiro dia da menstruação
                    </label>
                    <label htmlFor="flow-level">Fluxo</label>
                    <select
                      id="flow-level"
                      name="flowLevel"
                      defaultValue={editingPeriod?.flowLevel ?? 'LIGHT'}
                    >
                      <option value="LIGHT">Leve</option>
                      <option value="MEDIUM">Moderado</option>
                      <option value="HEAVY">Intenso</option>
                    </select>
                    <div className="period-actions">
                      <button className="primary-button" type="submit" disabled={busy}>
                        {editingPeriod ? 'Salvar alteração' : 'Registrar dia'}
                      </button>
                      {editingPeriod && (
                        <>
                          <button
                            className="secondary-button"
                            type="button"
                            onClick={() => void deletePeriodEntry(editingPeriod)}
                            disabled={busy}
                          >
                            Remover registro
                          </button>
                          <button
                            className="text-button"
                            type="button"
                            onClick={() => setEditingPeriod(null)}
                            disabled={busy}
                          >
                            Cancelar edição
                          </button>
                        </>
                      )}
                    </div>
                  </form>
                </>
              )}
            </section>

            <section className="panel" aria-labelledby="symptoms-heading">
              <h2 id="symptoms-heading">Sintomas e sinais corporais</h2>
              <p className="muted">
                Registre apenas o que fizer sentido para você. Intensidade subjetiva: 1 (leve) a 5 (muito intensa).
              </p>
              <ol className="entry-list">
                {symptomEntries.map((entry) => (
                  <li key={entry.id}>
                    <div className="entry-copy">
                      <strong>{formatPeriodDate(entry.entryDate)} · {symptomOptions.find((item) => item.value === entry.symptomType)?.label ?? entry.symptomType}</strong>
                      <span>Intensidade {entry.intensity} de 5</span>
                      {entry.notes && <span className="private-text">{entry.notes}</span>}
                    </div>
                    <div className="entry-actions">
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => setEditingSymptom(entry)}
                        disabled={busy}
                        aria-label={`Editar ${symptomOptions.find((item) => item.value === entry.symptomType)?.label ?? 'sintoma'} de ${formatPeriodDate(entry.entryDate)}`}
                      >
                        Editar
                      </button>
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => void deleteSymptomEntry(entry)}
                        disabled={busy}
                        aria-label={`Remover ${symptomOptions.find((item) => item.value === entry.symptomType)?.label ?? 'sintoma'} de ${formatPeriodDate(entry.entryDate)}`}
                      >
                        Remover
                      </button>
                    </div>
                  </li>
                ))}
                {symptomEntries.length === 0 && <li className="empty-list">Nenhum registro de sintoma neste mês.</li>}
              </ol>
              <form
                className="form-stack health-entry-form"
                onSubmit={saveSymptomEntry}
                key={editingSymptom?.id ?? 'new-symptom-entry'}
              >
                <h3>{editingSymptom ? 'Editar sintoma' : 'Registrar sintoma'}</h3>
                <label htmlFor="symptom-date">Data</label>
                <input
                  id="symptom-date"
                  name="symptomDate"
                  type="date"
                  max={todayDateKey()}
                  defaultValue={editingSymptom?.entryDate ?? todayDateKey()}
                  required
                />
                <label htmlFor="symptom-type">Sintoma ou sinal</label>
                <select
                  id="symptom-type"
                  name="symptomType"
                  defaultValue={editingSymptom?.symptomType ?? 'CRAMPS'}
                >
                  {symptomOptions.map((option) => (
                    <option value={option.value} key={option.value}>{option.label}</option>
                  ))}
                </select>
                <label htmlFor="symptom-intensity">Intensidade (1 a 5)</label>
                <select
                  id="symptom-intensity"
                  name="intensity"
                  defaultValue={String(editingSymptom?.intensity ?? 1)}
                  required
                >
                  <option value="1">1 — Leve</option>
                  <option value="2">2</option>
                  <option value="3">3 — Moderada</option>
                  <option value="4">4</option>
                  <option value="5">5 — Muito intensa</option>
                </select>
                <label htmlFor="symptom-notes">Observação opcional (até 2.000 caracteres)</label>
                <textarea
                  id="symptom-notes"
                  name="symptomNotes"
                  maxLength={2000}
                  rows={3}
                  defaultValue={editingSymptom?.notes ?? ''}
                />
                <div className="period-actions">
                  <button className="primary-button" type="submit" disabled={busy}>
                    {editingSymptom ? 'Salvar alteração' : 'Adicionar sintoma'}
                  </button>
                  {editingSymptom && (
                    <button className="text-button" type="button" onClick={() => setEditingSymptom(null)} disabled={busy}>
                      Cancelar edição
                    </button>
                  )}
                </div>
              </form>
            </section>

            <section className="panel" aria-labelledby="wellbeing-heading">
              <h2 id="wellbeing-heading">Humor e bem-estar</h2>
              <p className="muted">
                Entradas opcionais e independentes. As escalas são percepções pessoais, não avaliações clínicas.
              </p>
              <ol className="entry-list">
                {wellbeingEntries.map((entry) => (
                  <li key={entry.id}>
                    <div className="entry-copy">
                      <strong>{formatPeriodDate(entry.entryDate)} · {moodOptions.find((item) => item.value === entry.mood)?.label ?? entry.mood}</strong>
                      <span>
                        {[
                          entry.stressLevel !== null ? `Estresse ${entry.stressLevel}/5` : null,
                          entry.energyLevel !== null ? `Energia ${entry.energyLevel}/5` : null,
                          entry.sleepQuality !== null ? `Sono ${entry.sleepQuality}/5` : null,
                        ].filter(Boolean).join(' · ') || 'Sem escalas adicionais'}
                      </span>
                      {entry.reflection && <span className="private-text">{entry.reflection}</span>}
                    </div>
                    <div className="entry-actions">
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => setEditingWellbeing(entry)}
                        disabled={busy}
                        aria-label={`Editar registro de bem-estar de ${formatPeriodDate(entry.entryDate)}`}
                      >
                        Editar
                      </button>
                      <button
                        className="text-button"
                        type="button"
                        onClick={() => void deleteWellbeingEntry(entry)}
                        disabled={busy}
                        aria-label={`Remover registro de bem-estar de ${formatPeriodDate(entry.entryDate)}`}
                      >
                        Remover
                      </button>
                    </div>
                  </li>
                ))}
                {wellbeingEntries.length === 0 && <li className="empty-list">Nenhum registro de bem-estar neste mês.</li>}
              </ol>
              <form
                className="form-stack health-entry-form"
                onSubmit={saveWellbeingEntry}
                key={editingWellbeing?.id ?? 'new-wellbeing-entry'}
              >
                <h3>{editingWellbeing ? 'Editar registro' : 'Registrar como você está'}</h3>
                <label htmlFor="wellbeing-date">Data</label>
                <input
                  id="wellbeing-date"
                  name="wellbeingDate"
                  type="date"
                  max={todayDateKey()}
                  defaultValue={editingWellbeing?.entryDate ?? todayDateKey()}
                  required
                />
                <label htmlFor="mood-level">Humor</label>
                <select id="mood-level" name="mood" defaultValue={editingWellbeing?.mood ?? 'NEUTRAL'}>
                  {moodOptions.map((option) => (
                    <option value={option.value} key={option.value}>{option.label}</option>
                  ))}
                </select>
                <label htmlFor="stress-level">Estresse (1 = baixo, 5 = alto; opcional)</label>
                <select id="stress-level" name="stressLevel" defaultValue={editingWellbeing?.stressLevel?.toString() ?? ''}>
                  <option value="">Não registrar</option>
                  <option value="1">1 — Baixo</option>
                  <option value="2">2</option>
                  <option value="3">3 — Moderado</option>
                  <option value="4">4</option>
                  <option value="5">5 — Alto</option>
                </select>
                <label htmlFor="energy-level">Energia (1 = baixa, 5 = alta; opcional)</label>
                <select id="energy-level" name="energyLevel" defaultValue={editingWellbeing?.energyLevel?.toString() ?? ''}>
                  <option value="">Não registrar</option>
                  <option value="1">1 — Baixa</option>
                  <option value="2">2</option>
                  <option value="3">3 — Média</option>
                  <option value="4">4</option>
                  <option value="5">5 — Alta</option>
                </select>
                <label htmlFor="sleep-quality">Qualidade percebida do sono (1 = baixa, 5 = alta; opcional)</label>
                <select id="sleep-quality" name="sleepQuality" defaultValue={editingWellbeing?.sleepQuality?.toString() ?? ''}>
                  <option value="">Não registrar</option>
                  <option value="1">1 — Baixa</option>
                  <option value="2">2</option>
                  <option value="3">3 — Média</option>
                  <option value="4">4</option>
                  <option value="5">5 — Alta</option>
                </select>
                <label htmlFor="reflection">Reflexão privada opcional (até 2.000 caracteres)</label>
                <textarea
                  id="reflection"
                  name="reflection"
                  maxLength={2000}
                  rows={4}
                  defaultValue={editingWellbeing?.reflection ?? ''}
                />
                <div className="period-actions">
                  <button className="primary-button" type="submit" disabled={busy}>
                    {editingWellbeing ? 'Salvar alteração' : 'Adicionar registro'}
                  </button>
                  {editingWellbeing && (
                    <button className="text-button" type="button" onClick={() => setEditingWellbeing(null)} disabled={busy}>
                      Cancelar edição
                    </button>
                  )}
                </div>
              </form>
            </section>

            <section className="panel" aria-labelledby="export-heading">
              <h2 id="export-heading">Exportar seus dados</h2>
              <p>Confirme sua senha para gerar um arquivo local protegido contra cache.</p>
              <form className="form-stack" onSubmit={exportData}>
                <label htmlFor="export-password">Senha atual</label>
                <input id="export-password" name="password" type="password" autoComplete="current-password" required />
                <button className="secondary-button" type="submit" disabled={busy}>
                  Preparar exportação
                </button>
              </form>
              <p className="muted">Inclui perfil, consentimentos, calendário, sintomas e registros de bem-estar disponíveis.</p>
            </section>

            <section className="panel danger-panel" aria-labelledby="delete-heading">
              <h2 id="delete-heading">Excluir conta</h2>
              <p>A exclusão remove a conta, perfil, consentimentos e sessões associados.</p>
              <form className="form-stack" onSubmit={deleteAccount}>
                <label htmlFor="delete-password">Confirme sua senha</label>
                <input id="delete-password" name="password" type="password" autoComplete="current-password" required />
                <label className="checkbox-label">
                  <input type="checkbox" name="confirmed" required />
                  <span>Entendo que a exclusão é permanente.</span>
                </label>
                <button className="danger-button" type="submit" disabled={busy}>
                  Excluir conta e dados
                </button>
              </form>
            </section>
          </div>
        ) : (
          <section className="panel auth-panel" aria-labelledby="auth-heading">
            <div className="auth-tabs" role="group" aria-label="Acesso à conta">
              <button
                type="button"
                className={mode === 'login' ? 'tab-button active' : 'tab-button'}
                onClick={() => { setMode('login'); setError(''); setNotice('') }}
              >
                Entrar
              </button>
              <button
                type="button"
                className={mode === 'register' ? 'tab-button active' : 'tab-button'}
                onClick={() => { setMode('register'); setError(''); setNotice('') }}
              >
                Criar conta
              </button>
            </div>
            <h2 id="auth-heading">
              {mode === 'login' ? 'Acesse sua conta' : 'Comece com o essencial'}
            </h2>
            <form className="form-stack" onSubmit={submitAuth}>
              {mode === 'register' && (
                <>
                  <label htmlFor="register-name">Como gostaria de ser chamada?</label>
                  <input id="register-name" name="displayName" autoComplete="nickname" maxLength={80} required />
                </>
              )}
              <label htmlFor="auth-email">Email</label>
              <input id="auth-email" name="email" type="email" autoComplete="email" maxLength={254} required />
              <label htmlFor="auth-password">Senha</label>
              <input
                id="auth-password"
                name="password"
                type="password"
                autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                minLength={mode === 'register' ? 12 : undefined}
                maxLength={72}
                required
              />
              {mode === 'register' && (
                <p className="muted">Use pelo menos 12 caracteres. A recuperação de senha ainda não está disponível.</p>
              )}
              <button className="primary-button" type="submit" disabled={busy}>
                {busy ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar conta'}
              </button>
            </form>
          </section>
        )}
      </section>

      <footer className="page-footer">
        <span>Dados pessoais tratados com cuidado.</span>
        <span>Esta etapa não oferece diagnóstico ou orientação clínica.</span>
      </footer>
    </main>
  )
}

export default App
