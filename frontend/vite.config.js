import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import http from 'node:http'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:2026',
        changeOrigin: true,
        agent: new http.Agent({ keepAlive: true, keepAliveMsecs: 20000 })
      },
    },
  },
})
