/**
 * Keep these values identical to backend EventCategory enum.
 * FullCalendar uses `color` / `contrastColor` for event chips.
 */
export const EVENT_CATEGORIES = [
  { value: 'WORK', label: 'Work' },
  { value: 'PERSONAL', label: 'Personal' },
  { value: 'HEALTH', label: 'Health' },
  { value: 'OTHER', label: 'Other' },
]

export const CATEGORY_COLORS = {
  WORK: { color: '#1f4b3a', contrast: '#f4faf6' },
  PERSONAL: { color: '#c45c26', contrast: '#fff8f3' },
  HEALTH: { color: '#2f6f7e', contrast: '#f3fafb' },
  OTHER: { color: '#5d6f66', contrast: '#f5f7f6' },
}

export function normalizeCategory(category) {
  return CATEGORY_COLORS[category] ? category : 'OTHER'
}

export function colorsForCategory(category) {
  return CATEGORY_COLORS[normalizeCategory(category)]
}
