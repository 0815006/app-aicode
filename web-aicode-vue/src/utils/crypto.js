/**
 * 前端加解密公共方法
 * AES-CBC 加密，与后端 AesUtils（java-aicode-server/util/AesUtils.java）同 key/IV 互通
 * 用于数据库连接配置(dbPass)等密码类字段：前端加密后传输，后端密文入库，使用时解密访问
 */
import CryptoJS from 'crypto-js'

const KEY = CryptoJS.enc.Utf8.parse('ihaierForTodoKey')
const IV = CryptoJS.enc.Utf8.parse('ihaierForTodo_Iv')

/**
 * AES-CBC 加密密码
 * @param {string} plainText 明文；为空返回空串
 * @returns {string} Base64 密文
 */
export function encryptPassword(plainText) {
  if (!plainText) return ''
  const encrypted = CryptoJS.AES.encrypt(plainText, KEY, {
    iv: IV,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })
  return encrypted.toString()
}
