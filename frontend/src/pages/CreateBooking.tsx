import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Calendar, Clock, Package, CheckCircle } from 'lucide-react'
import { api, Resource, ResourceType } from '../services/api'
import toast from 'react-hot-toast'

export default function CreateBooking() {
  const [searchParams] = useSearchParams()
  const resourceIdParam = searchParams.get('resourceId')
  const navigate = useNavigate()

  const [resources, setResources] = useState<Resource[]>([])
  const [selectedResourceId, setSelectedResourceId] = useState<number | null>(
    resourceIdParam ? parseInt(resourceIdParam) : null
  )
  const [startTime, setStartTime] = useState('')
  const [endTime, setEndTime] = useState('')
  const [loading, setLoading] = useState(false)
  const [checkingAvailability, setCheckingAvailability] = useState(false)
  const [isAvailable, setIsAvailable] = useState<boolean | null>(null)

  useEffect(() => {
    loadResources()
  }, [])

  useEffect(() => {
    if (selectedResourceId && startTime && endTime) {
      checkAvailability()
    } else {
      setIsAvailable(null)
    }
  }, [selectedResourceId, startTime, endTime])

  const loadResources = async () => {
    try {
      const data = await api.resources.getAll()
      setResources(data)
    } catch (error) {
      toast.error('Ошибка загрузки ресурсов')
    }
  }

  const checkAvailability = async () => {
    if (!selectedResourceId || !startTime || !endTime) return

    setCheckingAvailability(true)
    try {
      const available = await api.bookings.checkAvailability(
        selectedResourceId,
        startTime,
        endTime
      )
      setIsAvailable(available)
      if (!available) {
        toast.error('Ресурс недоступен в выбранное время')
      }
    } catch (error) {
      toast.error('Ошибка проверки доступности')
    } finally {
      setCheckingAvailability(false)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!selectedResourceId || !startTime || !endTime) {
      toast.error('Заполните все поля')
      return
    }

    if (isAvailable === false) {
      toast.error('Ресурс недоступен в выбранное время')
      return
    }

    setLoading(true)
    try {
      await api.bookings.create({
        resourceId: selectedResourceId,
        startTime,
        endTime,
      })
      toast.success('Бронирование создано успешно!')
      navigate('/bookings')
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Ошибка создания бронирования')
    } finally {
      setLoading(false)
    }
  }

  const minDateTime = new Date().toISOString().slice(0, 16)

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Создать бронирование</h1>
        <p className="text-gray-600">Забронируйте ресурс на нужное время</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="card"
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              <Package className="w-4 h-4 inline mr-2" />
              Ресурс
            </label>
            <select
              value={selectedResourceId || ''}
              onChange={(e) => setSelectedResourceId(parseInt(e.target.value))}
              className="input-field"
              required
            >
              <option value="">Выберите ресурс</option>
              {resources.map((resource) => (
                <option key={resource.id} value={resource.id}>
                  {resource.name} ({mapType(resource.type)}) · {resource.capacity} мест · {resource.pricePerHour} ₸/час
                </option>
              ))}
            </select>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Clock className="w-4 h-4 inline mr-2" />
                Начало
              </label>
              <input
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                min={minDateTime}
                className="input-field"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Clock className="w-4 h-4 inline mr-2" />
                Окончание
              </label>
              <input
                type="datetime-local"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                min={startTime || minDateTime}
                className="input-field"
                required
              />
            </div>
          </div>

          {checkingAvailability && (
            <div className="flex items-center text-blue-600">
              <div className="animate-spin rounded-full h-5 w-5 border-t-2 border-b-2 border-blue-600 mr-2"></div>
              <span>Проверка доступности...</span>
            </div>
          )}

          {isAvailable === true && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex items-center p-4 bg-green-50 border border-green-200 rounded-lg"
            >
              <CheckCircle className="w-5 h-5 text-green-600 mr-2" />
              <span className="text-green-800 font-medium">Ресурс доступен в выбранное время</span>
            </motion.div>
          )}

          {isAvailable === false && (
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              className="flex items-center p-4 bg-red-50 border border-red-200 rounded-lg"
            >
              <Calendar className="w-5 h-5 text-red-600 mr-2" />
              <span className="text-red-800 font-medium">Ресурс недоступен в выбранное время</span>
            </motion.div>
          )}

          <div className="flex space-x-4">
            <motion.button
              type="submit"
              disabled={loading || isAvailable === false}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="btn-primary flex-1 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Создание...' : 'Создать бронирование'}
            </motion.button>
            <button
              type="button"
              onClick={() => navigate('/resources')}
              className="btn-secondary"
            >
              Отмена
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  )
}

const typeLabels: Record<ResourceType, string> = {
  MEETING_ROOM: 'Переговорка',
  CONFERENCE_HALL: 'Конференц-зал',
  EQUIPMENT: 'Оборудование',
  VEHICLE: 'Транспорт',
  WORKSPACE: 'Рабочее место',
  OTHER: 'Другое',
}

function mapType(type: ResourceType) {
  return typeLabels[type] ?? type
}

