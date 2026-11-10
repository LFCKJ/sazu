import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: true, // 스마트폰으로 로컬 개발 서버에 접속해 QR 스캔을 테스트하기 위한 필수 옵션
    proxy: {
      // 프론트에서 '/api'로 시작하는 요청을 보내면 자동으로 localhost:8080으로 프록시 전달
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
