import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Package, Tag, Users, DollarSign, CheckCircle } from 'lucide-react'
import toast from 'react-hot-toast'
import { api, ResourceType } from '../services/api'

const RESOURCE_TYPES: { value: ResourceType; label: string }[] = [
  { value: 'MEETING_ROOM', label: 'Переговорка' },
  { value: 'CONFERENCE_HALL', label: 'Конференц-зал' },
  { value: 'EQUIPMENT', label: 'Оборудование' },
  { value: 'VEHICLE', label: 'Транспорт' },
  { value: 'WORKSPACE', label: 'Рабочее место' },
  { value: 'OTHER', label: 'Другое' },
]

export default function CreateResource() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [files, setFiles] = useState<FileList | null>(null)
  const [form, setForm] = useState({
    name: '',
    description: '',
    type: 'MEETING_ROOM' as ResourceType,
    pricePerHour: '',
    capacity: '',
    available: true,
  })

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type, checked } = e.target as HTMLInputElement
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!form.name.trim()) {
      toast.error('Введите название')
      return
    }
    if (!form.pricePerHour || Number(form.pricePerHour) <= 0) {
      toast.error('Цена должна быть больше нуля')
      return
    }
    if (!form.capacity || Number(form.capacity) <= 0) {
      toast.error('Вместимость должна быть больше нуля')
      return
    }

    setLoading(true)
    try {
      const created = await api.resources.create({
        name: form.name,
        description: form.description,
        type: form.type,
        pricePerHour: Number(form.pricePerHour),
        capacity: Number(form.capacity),
        available: form.available,
      })

      // Загрузка прикреплённых файлов с привязкой к ресурсу
      if (files && files.length > 0) {
        for (const file of Array.from(files)) {
          await api.files.upload(file, created.id)
        }
      }

      toast.success('Ресурс создан')
      navigate('/resources')
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Ошибка создания ресурса')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-8"
      >
        <h1 className="text-4xl font-bold text-gray-800 mb-2">Создать ресурс</h1>
        <p className="text-gray-600">Добавьте новый ресурс, чтобы его можно было бронировать</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="card"
      >
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              <Package className="w-4 h-4 inline mr-2" />
              Название *
            </label>
            <input
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              className="input-field"
              placeholder="Например, Переговорная №1"
              required
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Tag className="w-4 h-4 inline mr-2" />
                Тип *
              </label>
              <select
                name="type"
                value={form.type}
                onChange={handleChange}
                className="input-field"
              >
                {RESOURCE_TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <DollarSign className="w-4 h-4 inline mr-2" />
                Цена за час (₸) *
              </label>
              <input
                type="number"
                name="pricePerHour"
                value={form.pricePerHour}
                onChange={handleChange}
                min={0}
                step={100}
                className="input-field"
                required
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                <Users className="w-4 h-4 inline mr-2" />
                Вместимость *
              </label>
              <input
                type="number"
                name="capacity"
                value={form.capacity}
                onChange={handleChange}
                min={1}
                className="input-field"
                required
              />
            </div>

            <div className="flex items-center space-x-3 mt-6">
              <input
                id="available"
                type="checkbox"
                name="available"
                checked={form.available}
                onChange={handleChange}
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label htmlFor="available" className="text-sm text-gray-700">
                Доступен для бронирования
              </label>
            </div>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Описание
            </label>
            <textarea
              name="description"
              value={form.description}
              onChange={handleChange}
              className="input-field min-h-[100px]"
              placeholder="Краткое описание, оснащение, адрес"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Прикрепить файлы (опционально)
            </label>
            <input
              type="file"
              multiple
              onChange={(e) => setFiles(e.target.files)}
              className="block w-full text-sm text-gray-700"
            />
            <p className="text-xs text-gray-500 mt-1">Файлы будут связаны с созданным ресурсом</p>
          </div>

          <div className="flex space-x-4">
            <motion.button
              type="submit"
              disabled={loading}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="btn-primary flex-1 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Сохранение...' : 'Создать ресурс'}
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

      <div className="mt-6 flex items-start space-x-3 text-sm text-gray-600">
        <CheckCircle className="w-5 h-5 text-green-600 mt-0.5" />
        <p>
          Для создания ресурса нужна роль администратора. Если запрос вернёт 403, войдите под
          админом или выдайте себе роль admin в Keycloak.
        </p>
      </div>
    </div>
  )
}


