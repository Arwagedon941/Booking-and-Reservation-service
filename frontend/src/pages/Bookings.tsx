import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Calendar, X, Clock, Package } from 'lucide-react'
import { api, Booking } from '../services/api'
import toast from 'react-hot-toast'
import { format } from 'date-fns'

const statusLabels: Record<string, string> = {
  PENDING: 'Ожидает',
  CONFIRMED: 'Подтверждено',
  CANCELLED: 'Отменено',
}

const statusColors: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-800',
  CONFIRMED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-red-100 text-red-800',
}

export default function Bookings() {
  const [bookings, setBookings] = useState<Booking[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadBookings()
  }, [])

  const loadBookings = async () => {
    try {
      const data = await api.bookings.getAll()
      setBookings(data.sort((a, b) => new Date(b.startTime).getTime() - new Date(a.startTime).getTime()))
    } catch (error) {
      toast.error('Ошибка загрузки бронирований')
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = async (id: number) => {
    if (!confirm('Вы уверены, что хотите отменить это бронирование?')) {
      return
    }

    try {
      await api.bookings.cancel(id)
      toast.success('Бронирование отменено')
      loadBookings()
    } catch (error) {
      toast.error('Ошибка отмены бронирования')
    }
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/4"></div>
          <div className="space-y-4">
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
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Мои бронирования</h1>
        <p className="text-gray-600">Управление вашими бронированиями</p>
      </motion.div>

      {bookings.length === 0 ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center py-16"
        >
          <Calendar className="w-16 h-16 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-600 text-lg mb-4">У вас пока нет бронирований</p>
        </motion.div>
      ) : (
        <div className="space-y-4">
          {bookings.map((booking, index) => (
            <motion.div
              key={booking.id}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.1 }}
              className="card"
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center mb-4">
                    <div className="p-3 bg-blue-100 rounded-lg mr-4">
                      <Package className="w-6 h-6 text-blue-600" />
                    </div>
                    <div>
                      <h3 className="text-xl font-bold text-gray-800">
                        Ресурс #{booking.resourceId}
                      </h3>
                      <span
                        className={`inline-block mt-2 px-3 py-1 rounded-full text-xs font-medium ${
                          statusColors[booking.status]
                        }`}
                      >
                        {statusLabels[booking.status]}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                    <div className="flex items-center text-gray-600">
                      <Clock className="w-5 h-5 mr-2 text-blue-600" />
                      <div>
                        <p className="text-sm font-medium">Начало</p>
                        <p className="font-semibold text-gray-800">
                          {format(new Date(booking.startTime), 'd MMM yyyy, HH:mm')}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center text-gray-600">
                      <Clock className="w-5 h-5 mr-2 text-blue-600" />
                      <div>
                        <p className="text-sm font-medium">Окончание</p>
                        <p className="font-semibold text-gray-800">
                          {format(new Date(booking.endTime), 'd MMM yyyy, HH:mm')}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                {booking.status === 'CONFIRMED' && (
                  <button
                    onClick={() => handleCancel(booking.id)}
                    className="ml-4 p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                    title="Отменить бронирование"
                  >
                    <X className="w-5 h-5" />
                  </button>
                )}
              </div>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  )
}

