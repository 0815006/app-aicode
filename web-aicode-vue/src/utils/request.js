// src/utils/request.js
import axios from 'axios'
import { getCurrentEmpNo } from '@/utils/currentUser'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    config.headers['token'] = getCurrentEmpNo()
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => {
    // blob / arraybuffer 类型直接返回完整响应对象（文件下载场景）
    if (response.config && (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer')) {
      return response
    }
    const res = response.data
    if (res.code !== 200) {
      // 处理错误
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  error => {
    return Promise.reject(error)
  }
)

export default request
