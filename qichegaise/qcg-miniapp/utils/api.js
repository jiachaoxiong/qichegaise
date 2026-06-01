var app = getApp()

function request(method, path, data) {
  data = data || {}
  if (!app.isLoggedIn()) {
    wx.navigateTo({ url: '/pages/login/login' })
    return Promise.reject(new Error('请先登录'))
  }
  return new Promise(function (resolve, reject) {
    function doRequest() {
      wx.request({
        url: app.globalData.baseUrl + path,
        method: method,
        header: app.getAuthHeader(),
        data: data,
        success: function (res) {
          var body = res.data
          if (body.code === 200) {
            resolve(body.data)
          } else if (res.statusCode === 401) {
            app.globalData.token = null
            wx.setStorageSync('token', '')
            wx.navigateTo({ url: '/pages/login/login' })
            reject(new Error('登录已过期'))
          } else {
            wx.showToast({ title: body.message || '请求失败', icon: 'none' })
            reject(new Error(body.message))
          }
        },
        fail: function () {
          wx.showToast({ title: '网络异常', icon: 'none' })
          reject(new Error('网络异常'))
        }
      })
    }
    doRequest()
  })
}

function uploadFile(filePath) {
  if (!app.isLoggedIn()) {
    wx.navigateTo({ url: '/pages/login/login' })
    return Promise.reject(new Error('请先登录'))
  }
  return new Promise(function (resolve, reject) {
    wx.uploadFile({
      url: app.globalData.baseUrl + '/api/photos/upload',
      filePath: filePath,
      name: 'file',
      header: { 'Authorization': 'Bearer ' + app.globalData.token },
      success: function (res) {
        var body = JSON.parse(res.data)
        if (body.code === 200) {
          resolve(body.data)
        } else {
          reject(new Error(body.message))
        }
      },
      fail: function () {
        wx.showToast({ title: '上传失败', icon: 'none' })
        reject(new Error('上传失败'))
      }
    })
  })
}

module.exports = {
  get: function (path) { return request('GET', path) },
  post: function (path, data) { return request('POST', path, data) },
  delete: function (path) { return request('DELETE', path) },
  uploadFile: uploadFile
}
