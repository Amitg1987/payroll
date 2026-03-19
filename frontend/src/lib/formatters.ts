const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 2,
})

const dateFormatter = new Intl.DateTimeFormat('en-US', {
  year: 'numeric',
  month: 'short',
  day: 'numeric',
})

const percentFormatter = new Intl.NumberFormat('en-US', {
  style: 'percent',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

export function formatCurrency(value: number | null | undefined): string {
  return currencyFormatter.format(value ?? 0)
}

export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return 'Not scheduled'
  }
  return dateFormatter.format(new Date(value))
}

export function formatPercent(value: number | null | undefined): string {
  return percentFormatter.format(value ?? 0)
}

export function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
