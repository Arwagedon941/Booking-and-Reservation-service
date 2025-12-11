import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// Создаем отдельный экземпляр axios для API запросов
const apiClient = axios.create({
  baseURL: API_BASE_URL,
})

// Настраиваем interceptor для автоматической отправки токена
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('[API] Request to', config.url, 'with token, length:', token.length)
    } else {
      console.warn('[API] Request to', config.url, 'without token')
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Interceptor для обработки ошибок
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error('[API] 401 Unauthorized - token may be invalid or missing')
      localStorage.removeItem('token')
    }
    return Promise.reject(error)
  }
)

export type ResourceType =
  | 'MEETING_ROOM'
  | 'CONFERENCE_HALL'
  | 'EQUIPMENT'
  | 'VEHICLE'
  | 'WORKSPACE'
  | 'OTHER'

export interface Resource {
  id: number
  name: string
  description: string
  type: ResourceType
  pricePerHour: number
  capacity: number
  available: boolean
}

export interface Booking {
  id: number
  resourceId: number
  userId: string
  startTime: string
  endTime: string
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  resource?: Resource
}

export interface FileInfo {
  name: string
}

export const api = {
  resources: {
    getAll: async (): Promise<Resource[]> => {
      const response = await apiClient.get('/resources')
      return response.data
    },
    getById: async (id: number): Promise<Resource> => {
      const response = await apiClient.get(`/resources/${id}`)
      return response.data
    },
    create: async (resource: Omit<Resource, 'id'>): Promise<Resource> => {
      const response = await apiClient.post('/resources', resource)
      return response.data
    },
    update: async (id: number, resource: Partial<Resource>): Promise<Resource> => {
      const response = await apiClient.put(`/resources/${id}`, resource)
      return response.data
    },
    delete: async (id: number): Promise<void> => {
      await apiClient.delete(`/resources/${id}`)
    },
  },
  bookings: {
    getAll: async (): Promise<Booking[]> => {
      const response = await apiClient.get('/bookings')
      return response.data
    },
    getById: async (id: number): Promise<Booking> => {
      const response = await apiClient.get(`/bookings/${id}`)
      return response.data
    },
    create: async (booking: Omit<Booking, 'id' | 'status' | 'userId'>): Promise<Booking> => {
      const response = await apiClient.post('/bookings', booking)
      return response.data
    },
    cancel: async (id: number): Promise<void> => {
      await apiClient.delete(`/bookings/${id}`)
    },
    checkAvailability: async (
      resourceId: number,
      startTime: string,
      endTime: string
    ): Promise<boolean> => {
      const response = await apiClient.get('/bookings/availability', {
        params: { resourceId, startTime, endTime },
      })
      return response.data
    },
  },
  files: {
    list: async (resourceId?: number): Promise<string[]> => {
      const response = await apiClient.get('/files', {
        params: resourceId ? { resourceId } : undefined,
      })
      return response.data
    },
    upload: async (file: File, resourceId?: number): Promise<string> => {
      const formData = new FormData()
      formData.append('file', file)
      if (resourceId !== undefined) {
        formData.append('resourceId', String(resourceId))
      }
      const response = await apiClient.post('/files', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      return response.data
    },
    download: async (name: string, resourceId?: number): Promise<Blob> => {
      const response = await apiClient.get(`/files/download`, {
        params: resourceId ? { name, resourceId } : { name },
        responseType: 'blob',
      })
      return response.data
    },
    delete: async (name: string): Promise<void> => {
      await apiClient.delete(`/files/${encodeURIComponent(name)}`)
    },
  },
}

