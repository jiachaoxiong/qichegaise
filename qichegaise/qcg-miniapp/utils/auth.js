const app = getApp()

function login() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          wx.request({
            url: app.globalData.baseUrl + '/api/auth/login',
            method: 'POST',
            data: { code: res.code },
            success(response) {
              const body = response.data
              if (body.code === 200 && body.data) {
                const { token, userId, nickname, avatarUrl } = body.data
                app.globalData.token = token
                app.globalData.userInfo = { userId, nickname, avatarUrl }
                wx.setStorageSync('token', token)
                resolve(body.data)
              } else {
                reject(new Error(body.message || '登录失败'))
              }
            },
            fail(err) {
              reject(new Error('网络异常，请检查后端服务是否启动'))
            }
          })
        } else {
          reject(new Error('获取微信 code 失败'))
        }
      },
      fail(err) {
        reject(err)
      }
    })
  })
}

function ensureLogin() {
  if (app.isLoggedIn()) {
    return Promise.resolve()
  }
  return login()
}

module.exports = { login, ensureLogin }
