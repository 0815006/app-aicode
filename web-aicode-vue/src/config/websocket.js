/**
 * WebSocket 基础地址配置
 *
 * 本地默认 ws://localhost:8092（同后端 Spring Boot server.port），
 * 部署时由 deploy_cloud_frontend.bat 通过 set VUE_APP_WS_BASE_URL=... 临时注入覆盖。
 *
 * 若需手动指定部署地址，直接修改下方 fallback 值后执行 npm run build 即可。
 */
export const WS_BASE_URL = process.env.VUE_APP_WS_BASE_URL || 'ws://22.189.27.79:8092'
