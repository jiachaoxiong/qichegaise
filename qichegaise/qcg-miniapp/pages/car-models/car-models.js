const api = require('../../utils/api')

Page({
  data: {
    brands: [],
    allModels: [],
    models: [],
    activeBrand: 'all'
  },

  onLoad() {
    api.get('/api/car-models').then(models => {
      const brands = [...new Set(models.map(m => m.brandName).filter(Boolean))].sort(function(a, b) { return a.localeCompare(b, 'zh') })
      this.setData({ brands, allModels: models, models })
    }).catch(() => {})
  },

  onBrandTap(e) {
    const brand = e.currentTarget.dataset.brand
    const models = brand === 'all'
      ? this.data.allModels
      : this.data.allModels.filter(m => m.brandName === brand)
    this.setData({ activeBrand: brand, models })
  },

  onSelectModel(e) {
    const model = e.currentTarget.dataset.model
    if (!model.imageUrl) {
      wx.showToast({ title: '该车型暂无可预览图片', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: '/pages/color-picker/color-picker?imageUrl=' +
           encodeURIComponent(model.imageUrl)
    })
  }
})
