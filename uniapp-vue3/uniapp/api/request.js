// 统一请求封装
const config = require('./config.js')

function request(url, method = 'GET', data = {}) {
  return new Promise((resolve, reject) => {
    const token = uni.getStorageSync('token')
    uni.request({
      url: config.baseUrl + url,
      method: method,
      data: data,
      timeout: config.timeout || 10000,
      header: {
        'Authorization': token ? 'Bearer ' + token : '',
        'Content-Type': 'application/json'
      },
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          uni.showToast({ title: '请重新登录', icon: 'none' })
          uni.redirectTo({ url: '/pages/login/login' })
          reject(res)
        } else {
          uni.showToast({ title: res.data.message || '请求失败', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  get: (url, data) => request(url, 'GET', data),
  post: (url, data) => request(url, 'POST', data),
  put: (url, data) => request(url, 'PUT', data),
  delete: (url, data) => request(url, 'DELETE', data),
  getBaseUrl: () => config.baseUrl
}
