// vue.config.js
module.exports = {
    devServer: {
      // 前端开发服务启动端口
      port: 8082,
  
      // 是否自动打开浏览器
      open: true,
  
      // 代理配置（解决跨域）
      proxy: {
        '/api': {
          target: 'http://localhost:8092', // 后端服务地址（与 application.yml 端口一致）
          changeOrigin: true,
          pathRewrite: {
            '^/api': '/api' // 可选：如果后端已有 /api 前缀，可不重写
          }
        }
      }
    }
  }
  