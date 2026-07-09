// 跨 HTTP/HTTPS 环境均可用的复制到剪贴板工具
// 在非安全上下文（HTTP）下自动降级为 execCommand('copy')

export default function copyToClipboard(text) {
  return new Promise(function (resolve, reject) {
    // 优先使用现代 Clipboard API（需要 Secure Context + 用户已授权）
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text)
        .then(resolve)
        .catch(reject)
      return
    }

    // 降级方案：execCommand（兼容 HTTP 环境和旧浏览器）
    var ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.left = '-9999px'
    ta.style.top = '-9999px'
    document.body.appendChild(ta)
    ta.focus()
    ta.select()
    try {
      var success = document.execCommand('copy')
      document.body.removeChild(ta)
      if (success) {
        resolve()
      } else {
        reject(new Error('execCommand copy returned false'))
      }
    } catch (e) {
      document.body.removeChild(ta)
      reject(e)
    }
  })
}
