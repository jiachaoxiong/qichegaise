const api = require('../../utils/api')

Page({
  data: {
    shopId: null,
    shop: {},
    cases: [],
    isFavorited: false
  },

  onLoad(options) {
    const shopId = options.id
    this.setData({ shopId })
    this.loadDetail()
    this.loadCases()
    this.checkFavorite()
  },

  loadDetail() {
    api.get('/api/shops/' + this.data.shopId).then(shop => {
      this.setData({ shop })
    })
  },

  loadCases() {
    api.get('/api/shop-cases/shop/' + this.data.shopId).then(cases => {
      this.setData({ cases: cases || [] })
    })
  },

  checkFavorite() {
    api.get('/api/shops/favorites').then(shops => {
      const faved = (shops || []).some(s => s.id == this.data.shopId)
      this.setData({ isFavorited: faved })
    }).catch(() => {})
  },

  onToggleFavorite() {
    const { shopId, isFavorited } = this.data
    const method = isFavorited ? 'delete' : 'post'
    api[method]('/api/shops/' + shopId + '/favorite').then(() => {
      this.setData({ isFavorited: !isFavorited })
      wx.showToast({
        title: isFavorited ? '已取消收藏' : '已收藏',
        icon: 'none'
      })
    })
  }
})
