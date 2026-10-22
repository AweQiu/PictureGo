import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

const proxy = {
  // 注意必须用 '/picgo/' 前缀：'/picgo' 会误匹配 '/picgo.png' 等静态资源
  '/picgo/': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: { proxy },
  preview: { proxy },
})
