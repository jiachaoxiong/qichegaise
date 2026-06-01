const api = require('../../utils/api')

Page({
  data: { works: [] },

  onShow() {
    this.loadWorks()
  },

  loadWorks() {
    api.get('/api/photos').then(works => {
      this.setData({ works: works || [] })
    }).catch(() => {})
  },

  onTapWork(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=' + id
    })
  },

  onViewResult(e) {
    const item = e.currentTarget.dataset.item
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?photoId=' + item.id
    })
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '确认删除',
      content: '删除后无法恢复',
      success: (res) => {
        if (res.confirm) {
          api.delete('/api/photos/' + id).then(() => {
            this.loadWorks()
          })
        }
      }
    })
  }
})
