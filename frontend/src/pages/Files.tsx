import { useEffect, useMemo, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { Upload, File as FileIcon, Trash2, Download, RefreshCw } from 'lucide-react'
import toast from 'react-hot-toast'
import { api } from '../services/api'

function useIsAdmin() {
  return useMemo(() => {
    try {
      const token = localStorage.getItem('token')
      if (!token) return false
      const [, payloadBase64] = token.split('.')
      const payload = JSON.parse(atob(payloadBase64))
      const roles: string[] = payload?.realm_access?.roles || []
      return roles.includes('admin')
    } catch (e) {
      console.warn('Cannot parse token roles', e)
      return false
    }
  }, [])
}

export default function Files() {
  const [files, setFiles] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const inputRef = useRef<HTMLInputElement | null>(null)
  const isAdmin = useIsAdmin()

  useEffect(() => {
    loadFiles()
  }, [])

  const loadFiles = async () => {
    try {
      setLoading(true)
      const data = await api.files.list()
      setFiles(data)
    } catch (error) {
      console.error(error)
      toast.error('Не удалось загрузить список файлов')
    } finally {
      setLoading(false)
    }
  }

  const handleUpload = async (file?: File) => {
    if (!file) return
    try {
      setUploading(true)
      await api.files.upload(file)
      toast.success('Файл загружен')
      await loadFiles()
    } catch (error) {
      console.error(error)
      toast.error('Ошибка загрузки файла')
    } finally {
      setUploading(false)
      if (inputRef.current) {
        inputRef.current.value = ''
      }
    }
  }

  const handleDownload = async (name: string) => {
    try {
      const blob = await api.files.download(name)
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = name
      a.click()
      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error(error)
      toast.error('Ошибка скачивания')
    }
  }

  const handleDelete = async (name: string) => {
    if (!window.confirm(`Удалить файл ${name}?`)) return
    try {
      await api.files.delete(name)
      toast.success('Файл удален')
      await loadFiles()
    } catch (error) {
      console.error(error)
      toast.error('Ошибка удаления файла')
    }
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="mb-6 flex items-center justify-between"
      >
        <div>
          <h1 className="text-3xl font-bold text-gray-800 mb-1">Файлы</h1>
          <p className="text-gray-600">Загрузка, просмотр и управление файлами в MinIO</p>
        </div>
        <div className="flex space-x-2">
          <button
            onClick={loadFiles}
            className="btn-secondary inline-flex items-center"
            disabled={loading}
          >
            <RefreshCw className="w-4 h-4 mr-2" />
            Обновить
          </button>
          <label className="btn-primary inline-flex items-center cursor-pointer">
            <Upload className="w-5 h-5 mr-2" />
            {uploading ? 'Загрузка...' : 'Загрузить'}
            <input
              ref={inputRef}
              type="file"
              className="hidden"
              onChange={(e) => handleUpload(e.target.files?.[0])}
              disabled={uploading}
            />
          </label>
        </div>
      </motion.div>

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="h-20 bg-gray-100 rounded-lg animate-pulse" />
          ))}
        </div>
      ) : files.length === 0 ? (
        <div className="text-center py-16 text-gray-500">
          <FileIcon className="w-12 h-12 mx-auto mb-3 text-gray-400" />
          Файлов нет — загрузите первый.
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {files.map((name, idx) => (
            <motion.div
              key={name}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.05 }}
              className="flex items-center justify-between p-4 bg-white rounded-lg shadow-sm border"
            >
              <div className="flex items-center space-x-3">
                <FileIcon className="w-5 h-5 text-blue-600" />
                <span className="font-medium text-gray-800 break-all">{name}</span>
              </div>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => handleDownload(name)}
                  className="btn-secondary px-3 py-2 text-sm inline-flex items-center"
                >
                  <Download className="w-4 h-4 mr-1" />
                  Скачать
                </button>
                {isAdmin && (
                  <button
                    onClick={() => handleDelete(name)}
                    className="px-3 py-2 text-sm inline-flex items-center bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors"
                  >
                    <Trash2 className="w-4 h-4 mr-1" />
                    Удалить
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


