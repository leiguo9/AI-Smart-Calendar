export const EVENT_CATEGORIES = [
  { value: 'WORK', label: 'Work' },
  { value: 'PERSONAL', label: 'Personal' },
  { value: 'HEALTH', label: 'Health' },
  { value: 'OTHER', label: 'Other' },
]

export const CATEGORY_COLORS = {
  WORK: { background: '#1f4b3a', border: '#16382c' },
  PERSONAL: { background: '#c45c26', border: '#9a451c' },
  HEALTH: { background: '#2f6f7e', border: '#245863' },
  OTHER: { background: '#5d6f66', border: '#46554e' },
}

export function normalizeCategory(category) {
  return CATEGORY_COLORS[category] ? category : 'OTHER'
}

export function colorsForCategory(category) {
  return CATEGORY_COLORS[normalizeCategory(category)]
}
