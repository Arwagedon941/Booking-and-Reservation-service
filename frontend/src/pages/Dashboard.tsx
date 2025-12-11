import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Calendar, Package, TrendingUp, Clock } from 'lucide-react'
import { api, Booking } from '../services/api'
import toast from 'react-hot-toast'
import { format } from 'date-fns'

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalResources: 0,
    totalBookings: 0,
    upcomingBookings: 0,
  })
  const [recentBookings, setRecentBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    try {
      const [resources, bookings] = await Promise.all([
        api.resources.getAll(),
        api.bookings.getAll(),
      ])

      const now = new Date()
      const upcoming = bookings.filter(
        (b) => new Date(b.startTime) > now && b.status === 'CONFIRMED'
      )

      setStats({
        totalResources: resources.length,
        totalBookings: bookings.length,
        upcomingBookings: upcoming.length,
      })

      setRecentBookings(
        bookings
          .filter((b) => b.status === 'CONFIRMED')
          .sort((a, b) => new Date(a.startTime).getTime() - new Date(b.startTime).getTime())
          .slice(0, 5)
      )
    } catch (error) {
      toast.error('Ошибка загрузки данных')
    } finally {
      setLoading(false)
    }
  }

  const statCards = [
    {
      title: 'Всего ресурсов',
      value: stats.totalResources,
      icon: Package,
      color: 'from-blue-500 to-cyan-500',
    },
    {
      title: 'Всего бронирований',
      value: stats.totalBookings,
      icon: Calendar,
      color: 'from-purple-500 to-pink-500',
    },
    {
      title: 'Предстоящие',
      value: stats.upcomingBookings,
      icon: TrendingUp,
      color: 'from-green-500 to-emerald-500',
    },
  ]

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-32 bg-gray-200 rounded-xl"></div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Панель управления</h1>
        <p className="text-gray-600">Обзор вашей системы бронирования</p>
      </motion.div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        {statCards.map((stat, index) => {
          const Icon = stat.icon
          return (
            <motion.div
              key={stat.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              className="card"
            >
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-gray-600 mb-1">{stat.title}</p>
                  <p className="text-3xl font-bold text-gray-800">{stat.value}</p>
                </div>
                <div className={`p-3 rounded-lg bg-gradient-to-r ${stat.color}`}>
                  <Icon className="w-6 h-6 text-white" />
                </div>
              </div>
            </motion.div>
          )
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <motion.div
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.3 }}
          className="card"
        >
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-xl font-bold text-gray-800">Ближайшие бронирования</h2>
            <Link
              to="/bookings"
              className="text-blue-600 hover:text-blue-700 text-sm font-medium"
            >
              Посмотреть все
            </Link>
          </div>
          {recentBookings.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <Calendar className="w-12 h-12 mx-auto mb-2 opacity-50" />
              <p>Нет предстоящих бронирований</p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentBookings.map((booking, index) => (
                <motion.div
                  key={booking.id}
                  initial={{ opacity: 0, x: -10 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.4 + index * 0.1 }}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
                >
                  <div className="flex items-center">
                    <Clock className="w-5 h-5 text-blue-600 mr-3" />
                    <div>
                      <p className="font-medium text-gray-800">
                        {format(new Date(booking.startTime), 'd MMM, HH:mm')}
                      </p>
                      <p className="text-sm text-gray-600">
                        Ресурс #{booking.resourceId}
                      </p>
                    </div>
                  </div>
                  <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">
                    Подтверждено
                  </span>
                </motion.div>
              ))}
            </div>
          )}
        </motion.div>

        <motion.div
          initial={{ opacity: 0, x: 20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ delay: 0.4 }}
          className="card"
        >
          <h2 className="text-xl font-bold text-gray-800 mb-4">Быстрые действия</h2>
          <div className="space-y-3">
            <Link
              to="/bookings/create"
              className="block w-full btn-primary text-center"
            >
              Создать бронирование
            </Link>
            <Link
              to="/resources"
              className="block w-full btn-secondary text-center"
            >
              Просмотреть ресурсы
            </Link>
          </div>
        </motion.div>
      </div>
    </div>
  )
}

