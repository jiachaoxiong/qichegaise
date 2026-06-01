// pages/login/login.js
var auth = require('../../utils/auth')

Page({
  data: {
    loading: false,
    loginError: ''
  },

  onLoad: function () {
    if (getApp().isLoggedIn()) {
      wx.switchTab({ url: '/pages/index/index' })
    }
  },

  onLogin: function () {
    var that = this
    if (that.data.loading) return
    that.setData({ loading: true, loginError: '' })

    auth.login().then(function () {
      that.setData({ loading: false })
      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(function () {
        wx.switchTab({ url: '/pages/index/index' })
      }, 800)
    }).catch(function (err) {
      that.setData({
        loading: false,
        loginError: err.message || '登录失败，请重试'
      })
    })
  }
})
