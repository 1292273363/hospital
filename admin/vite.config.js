import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'

export default defineConfig({
  plugins: [vue()],
  base: '/admin/',
  build: {
    outDir: path.resolve(__dirname, '../backend/src/main/resources/static/admin'),
    emptyOutDir: true,
    // 让生产产物更贴近后端静态目录习惯（index.html 在 /admin/ 下）
    assetsDir: 'assets'
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // 开发环境：让前端请求 /admin/api/* 自动转发到后端
      '/admin/api': 'http://localhost:8080'
    }
  }
})

