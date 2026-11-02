import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

// 开发端口 5173 与后端 CALLBACK_ADDRESS（GitHub 回调）保持一致；/api 代理到后端 8808 并去掉前缀
export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8808',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        // 浏览器经代理访问属于同源，去掉 Origin 头，避免后端 CORS 白名单（FRONTEND_URL）不含 5173 时返回空 body 的 403
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        },
      },
    },
  },
})
