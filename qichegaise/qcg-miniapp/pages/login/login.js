// pages/login/login.js
const auth = require('../../utils/auth')

Page({
  data: {
    loading: false,
    loginError: ''
  },

  onLoad() {
    // 如果已登录，直接跳到首页
    if (getApp().isLoggedIn()) {
      wx.switchTab({ url: '/pages/index/index' })
    }
  },

  async onLogin() {
    if (this.data.loading) return
    this.setData({ loading: true, loginError: '' })

    try {
      await auth.login()
      this.setData({ loading: false })
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' })
      }, 800)
    } catch (err) {
      this.setData({
        loading: false,
        loginError: err.message || '登录失败，请重试'
      })
    }
  }
})
