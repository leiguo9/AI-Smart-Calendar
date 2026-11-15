import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Dev-only: browser calls /api/... on :5173; Vite forwards to the Spring Boot server.
    // Avoids CORS hassle during local npm run dev.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
