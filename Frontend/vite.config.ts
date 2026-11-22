import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Porta 5173 (default FRONTEND_URL do backend — CORS já liberado).
// O backend fica em http://localhost:8080 e é acessado via API_BASE em src/api/client.ts.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
})
