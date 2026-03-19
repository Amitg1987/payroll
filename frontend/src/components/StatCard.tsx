interface StatCardProps {
  label: string
  value: string | number
  hint: string
}

export function StatCard({ label, value, hint }: StatCardProps) {
  return (
    <article className="stat-card">
      <p className="eyebrow">{label}</p>
      <strong>{value}</strong>
      <span className="muted">{hint}</span>
    </article>
  )
}
