import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

/** Dev server: el front habla con /api y /uploads; Vite los manda al backend 8080. */
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://127.0.0.1:8080',
      '/uploads': 'http://127.0.0.1:8080',
    },
  },
})
