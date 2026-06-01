const app = getApp()
const auth = require('./auth')

function request(method, path, data = {}) {
  // 手动登录：未登录时跳转登录页
  if (!getApp().isLoggedIn()) {
    wx.navigateTo({ url: '/pages/login/login' })
    return Promise.reject(new Error('请先登录'))
  }
  return new Promise((resolve, reject) => {
      function doRequest() {
        wx.request({
          url: app.globalData.baseUrl + path,
          method: method,
          header: app.getAuthHeader(),
          data: data,
          success(res) {
            const body = res.data
            if (body.code === 200) {
              resolve(body.data)
            } else if (res.statusCode === 401) {
              auth.login().then(() => doRequest()).catch(reject)
            } else {
              wx.showToast({ title: body.message || '请求失败', icon: 'none' })
              reject(new Error(body.message))
            }
          },
          fail(err) {
            wx.showToast({ title: '网络异常', icon: 'none' })
            reject(err)
          }
        })
      }
      doRequest()
    })
}

function uploadFile(filePath) {
  if (!getApp().isLoggedIn()) {
    wx.navigateTo({ url: '/pages/login/login' })
    return Promise.reject(new Error('请先登录'))
  }
  return new Promise((resolve, reject) => {
      wx.uploadFile({
        url: app.globalData.baseUrl + '/api/photos/upload',
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + app.globalData.token
        },
        success(res) {
          const body = JSON.parse(res.data)
          if (body.code === 200) {
            resolve(body.data)
          } else {
            reject(new Error(body.message))
          }
        },
        fail(err) {
          wx.showToast({ title: '上传失败', icon: 'none' })
          reject(err)
        }
      })
    })
}

module.exports = {
  get: (path) => request('GET', path),
  post: (path, data) => request('POST', path, data),
  delete: (path) => request('DELETE', path),
  uploadFile
}
