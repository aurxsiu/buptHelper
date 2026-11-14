import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      // 开发期把 /api 转发到 Spring Boot 后端，避免跨域
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      input: {
        login: fileURLToPath(new URL('./index.html', import.meta.url)),
        register: fileURLToPath(new URL('./register.html', import.meta.url)),
        home: fileURLToPath(new URL('./home.html', import.meta.url)),
        profile: fileURLToPath(new URL('./profile.html', import.meta.url)),
        requests: fileURLToPath(new URL('./requests.html', import.meta.url)),
        requestEdit: fileURLToPath(new URL('./request-edit.html', import.meta.url)),
        requestDetail: fileURLToPath(new URL('./request-detail.html', import.meta.url)),
        responses: fileURLToPath(new URL('./responses.html', import.meta.url)),
        myResponses: fileURLToPath(new URL('./my-responses.html', import.meta.url)),
        responseEdit: fileURLToPath(new URL('./response-edit.html', import.meta.url)),
      },
    },
  },
})
