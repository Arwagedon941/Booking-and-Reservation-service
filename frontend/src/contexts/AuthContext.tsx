import React, { createContext, useContext, useState, useEffect } from 'react'
import axios from 'axios'

interface AuthContextType {
  isAuthenticated: boolean
  token: string | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (token) {
      // Обновляем токен в localStorage и axios defaults
      localStorage.setItem('token', token)
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
      setIsAuthenticated(true)
      console.log('[Auth] Token loaded from localStorage, length:', token.length)
    } else {
      // Удаляем токен если его нет
      localStorage.removeItem('token')
      delete axios.defaults.headers.common['Authorization']
      setIsAuthenticated(false)
    }
    setLoading(false)
  }, [token])

  const login = async (username: string, password: string) => {
    try {
      const formData = new URLSearchParams()
      formData.append('client_id', 'api-gateway')
      formData.append('client_secret', 'gateway-secret')
      formData.append('grant_type', 'password')
      formData.append('username', username)
      formData.append('password', password)

      // Используем API Gateway для обхода CORS
      const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      const response = await axios.post(
        `${apiUrl}/auth/realms/app/protocol/openid-connect/token`,
        formData.toString(), // Преобразуем URLSearchParams в строку
        {
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
          },
          withCredentials: true, // Важно для CORS
        }
      )

      const accessToken = response.data.access_token
      console.log('Token received, length:', accessToken?.length)
      console.log('Token preview:', accessToken?.substring(0, 50) + '...')
      
      setToken(accessToken)
      localStorage.setItem('token', accessToken)
      axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`
      setIsAuthenticated(true)
      
      console.log('Token saved to localStorage and axios defaults')
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    }
  }

  const logout = () => {
    setToken(null)
    localStorage.removeItem('token')
    delete axios.defaults.headers.common['Authorization']
    setIsAuthenticated(false)
  }

  return (
    <AuthContext.Provider value={{ isAuthenticated, token, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

