App({
  globalData: {
    token: null,
    userInfo: null,
    baseUrl: 'http://localhost:8080'
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  },

  isLoggedIn() {
    return !!this.globalData.token
  },

  getAuthHeader() {
    return {
      'Authorization': 'Bearer ' + (this.globalData.token || ''),
      'Content-Type': 'application/json'
    }
  }
})
