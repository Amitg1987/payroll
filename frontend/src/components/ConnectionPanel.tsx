import type { ConnectionSettings } from '../api/types'

interface ConnectionPanelProps {
  settings: ConnectionSettings
  onChange: (settings: ConnectionSettings) => void
  onConnect: () => void
  loading: boolean
}

export function ConnectionPanel({
  settings,
  onChange,
  onConnect,
  loading,
}: ConnectionPanelProps) {
  return (
    <div className="connection-panel">
      <div className="field-grid field-grid--compact">
        <label>
          API base URL
          <input
            value={settings.baseUrl}
            onChange={(event) =>
              onChange({ ...settings, baseUrl: event.target.value })
            }
            placeholder="http://localhost:8080"
          />
        </label>
        <label>
          Username
          <input
            value={settings.username}
            onChange={(event) =>
              onChange({ ...settings, username: event.target.value })
            }
          />
        </label>
        <label>
          Password
          <input
            type="password"
            value={settings.password}
            onChange={(event) =>
              onChange({ ...settings, password: event.target.value })
            }
          />
        </label>
      </div>
      <div className="connection-panel__actions">
        <button type="button" onClick={onConnect} disabled={loading}>
          {loading ? 'Connecting...' : 'Connect / Refresh'}
        </button>
        <p className="muted">
          Demo users: admin / Admin@123, accountant / Accountant@123, approver / Approver@123
        </p>
      </div>
    </div>
  )
}
