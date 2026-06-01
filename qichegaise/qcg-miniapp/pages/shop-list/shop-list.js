const api = require('../../utils/api')

Page({
  data: { shops: [] },

  onShow() {
    api.get('/api/shops').then(shops => {
      this.setData({ shops: shops || [] })
    }).catch(() => {})
  },

  onTapShop(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/shop-detail/shop-detail?id=' + id })
  }
})
