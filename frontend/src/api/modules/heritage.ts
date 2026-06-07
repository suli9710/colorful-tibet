export const heritageEndpoints = {
  list: '/heritage',
  detail: (id: number) => `/heritage/${id}`,
  like: (id: number) => `/heritage/${id}/like`,
  likeStatus: (id: number) => `/heritage/${id}/like-status`,
  comments: (id: number) => `/heritage/${id}/comments`,
  deleteComment: (heritageId: number, commentId: number) => `/heritage/${heritageId}/comments/${commentId}`,
  inheritors: (id: number) => `/heritage/${id}/inheritors`,
  events: (id: number) => `/heritage/${id}/events`,
  upcomingEvents: '/heritage/events/upcoming'
}

export const adminHeritageEndpoints = {
  list: '/admin/heritage',
  create: '/admin/heritage',
  update: (id: number) => `/admin/heritage/${id}`,
  delete: (id: number) => `/admin/heritage/${id}`,
  inheritors: (itemId: number) => `/admin/heritage/${itemId}/inheritors`,
  updateInheritor: (id: number) => `/admin/heritage/inheritors/${id}`,
  deleteInheritor: (id: number) => `/admin/heritage/inheritors/${id}`,
  events: (itemId: number) => `/admin/heritage/${itemId}/events`,
  updateEvent: (id: number) => `/admin/heritage/events/${id}`,
  deleteEvent: (id: number) => `/admin/heritage/events/${id}`
}
