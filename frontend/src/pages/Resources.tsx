import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Package, Plus, Users } from 'lucide-react'
import { api, Resource, ResourceType } from '../services/api'
import toast from 'react-hot-toast'

const resourceTypeLabels: Record<ResourceType, string> = {
  MEETING_ROOM: 'Переговорка',
  CONFERENCE_HALL: 'Конференц-зал',
  EQUIPMENT: 'Оборудование',
  VEHICLE: 'Транспорт',
  WORKSPACE: 'Рабочее место',
  OTHER: 'Другое',
}

const resourceTypeColors: Record<ResourceType, string> = {
  MEETING_ROOM: 'bg-blue-100 text-blue-800',
  CONFERENCE_HALL: 'bg-indigo-100 text-indigo-800',
  EQUIPMENT: 'bg-purple-100 text-purple-800',
  VEHICLE: 'bg-green-100 text-green-800',
  WORKSPACE: 'bg-amber-100 text-amber-800',
  OTHER: 'bg-gray-100 text-gray-800',
}

export default function Resources() {
  const [resources, setResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<string>('ALL')

  useEffect(() => {
    loadResources()
  }, [])

  const loadResources = async () => {
    try {
      const data = await api.resources.getAll()
      setResources(data)
    } catch (error) {
      toast.error('Ошибка загрузки ресурсов')
    } finally {
      setLoading(false)
    }
  }

  const filteredResources =
    filter === 'ALL'
      ? resources
      : resources.filter((r) => r.type === filter)

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="animate-pulse space-y-4">
          <div className="h-8 bg-gray-200 rounded w-1/4"></div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3, 4, 5, 6].map((i) => (
              <div key={i} className="h-64 bg-gray-200 rounded-xl"></div>
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
        <div className="flex items-center justify-between mb-4">
          <div>
            <h1 className="text-4xl font-bold text-gray-800 mb-2">Ресурсы</h1>
            <p className="text-gray-600">Просмотр и управление доступными ресурсами</p>
          </div>
          <div className="flex space-x-3">
            <Link
              to="/resources/create"
              className="btn-secondary inline-flex items-center"
            >
              <Plus className="w-5 h-5 mr-2" />
              Добавить ресурс
            </Link>
            <Link
              to="/bookings/create"
              className="btn-primary inline-flex items-center"
            >
              <Plus className="w-5 h-5 mr-2" />
              Забронировать
            </Link>
          </div>
        </div>

        <div className="flex space-x-2">
          {['ALL', 'MEETING_ROOM', 'CONFERENCE_HALL', 'EQUIPMENT', 'VEHICLE', 'WORKSPACE', 'OTHER'].map((type) => (
            <button
              key={type}
              onClick={() => setFilter(type)}
              className={`px-4 py-2 rounded-lg font-medium transition-all duration-200 ${
                filter === type
                  ? 'bg-blue-600 text-white shadow-lg'
                  : 'bg-white text-gray-700 hover:bg-gray-100'
              }`}
            >
                  {type === 'ALL' ? 'Все' : resourceTypeLabels[type as ResourceType]}
            </button>
          ))}
        </div>
      </motion.div>

      {filteredResources.length === 0 ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="text-center py-16"
        >
          <Package className="w-16 h-16 mx-auto text-gray-400 mb-4" />
          <p className="text-gray-600 text-lg">Ресурсы не найдены</p>
        </motion.div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredResources.map((resource, index) => (
            <motion.div
              key={resource.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              whileHover={{ y: -5 }}
              className="card cursor-pointer"
            >
              <div className="flex items-start justify-between mb-4">
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-gray-800 mb-2">{resource.name}</h3>
                  <span
                    className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${resourceTypeColors[resource.type]}`}
                  >
                    {resourceTypeLabels[resource.type]}
                  </span>
                </div>
                <Package className="w-8 h-8 text-blue-600" />
              </div>

              <p className="text-gray-600 mb-4 line-clamp-2">{resource.description}</p>

              <div className="flex items-center text-sm text-gray-600 space-x-4">
                <div className="flex items-center">
                  <Users className="w-4 h-4 mr-1" />
                  <span>Вместимость: {resource.capacity}</span>
                </div>
                <div className="text-sm font-semibold text-blue-700">
                  {resource.pricePerHour} ₸/час
                </div>
              </div>

              <Link
                to={`/bookings/create?resourceId=${resource.id}`}
                className="mt-4 block w-full text-center btn-primary"
              >
                Забронировать
              </Link>
            </motion.div>
          ))}
        </div>
      )}
    </div>
  )
}

