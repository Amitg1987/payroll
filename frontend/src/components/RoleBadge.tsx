import { titleCase } from '../lib/formatters'

interface RoleBadgeProps {
  role: string
}

export function RoleBadge({ role }: RoleBadgeProps) {
  return <span className={`role-badge role-badge--${role.toLowerCase()}`}>{titleCase(role)}</span>
}
