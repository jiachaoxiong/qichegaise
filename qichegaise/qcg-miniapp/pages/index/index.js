const api = require('../../utils/api')

Page({
  data: {
    recentWorks: []
  },

  onShow() {
    this.loadRecentWorks()
  },

  loadRecentWorks() {
    api.get('/api/photos').then(data => {
      this.setData({ recentWorks: (data || []).slice(0, 5) })
    }).catch(() => {})
  },

  onUploadPhoto() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success(res) {
        const tempFilePath = res.tempFiles[0].tempFilePath
        wx.showLoading({ title: '上传中...' })

        api.uploadFile(tempFilePath).then(data => {
          wx.hideLoading()
          wx.navigateTo({
            url: '/pages/color-picker/color-picker?photoId=' + data.id +
                 '&imageUrl=' + encodeURIComponent(data.originalUrl)
          })
        }).catch(() => {
          wx.hideLoading()
        })
      }
    })
  },

  onSelectModel() {
    wx.switchTab({ url: '/pages/car-models/car-models' })
  },

  onTapWork(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=' + id
    })
  }
})
