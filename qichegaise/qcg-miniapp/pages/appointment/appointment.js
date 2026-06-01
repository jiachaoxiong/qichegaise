const api = require('../../utils/api')

Page({
  data: {
    selectedShop: null,
    selectedColor: null,
    appointmentTime: '',
    remark: '',
    canSubmit: false
  },

  onLoad(options) {
    if (options.shopId) {
      api.get('/api/shops/' + options.shopId).then(shop => {
        this.setData({ selectedShop: shop })
        this.checkCanSubmit()
      })
    }
  },

  onPickShop() {
    api.get('/api/shops').then(shops => {
      const items = shops.map(s => s.name)
      wx.showActionSheet({
        itemList: items,
        success: (res) => {
          this.setData({ selectedShop: shops[res.tapIndex] })
          this.checkCanSubmit()
        }
      })
    })
  },

  onPickColor() {
    api.get('/api/colors').then(colors => {
      const items = colors.map(c => c.name)
      wx.showActionSheet({
        itemList: items,
        success: (res) => {
          this.setData({ selectedColor: colors[res.tapIndex] })
        }
      })
    })
  },

  onPickTime() {
    wx.showModal({
      title: '预约时间',
      content: '请输入期望的预约时间\n（如：2026-06-15 14:00）',
      editable: true,
      placeholderText: '2026-06-15 14:00',
      success: (res) => {
        if (res.confirm && res.content) {
          this.setData({ appointmentTime: res.content })
          this.checkCanSubmit()
        }
      }
    })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  checkCanSubmit() {
    const { selectedShop, appointmentTime } = this.data
    this.setData({ canSubmit: !!(selectedShop && appointmentTime) })
  },

  onSubmit() {
    const { selectedShop, selectedColor, appointmentTime, remark } = this.data
    wx.showLoading({ title: '提交中...' })
    api.post('/api/appointments', {
      shopId: selectedShop.id,
      colorId: selectedColor ? selectedColor.id : null,
      appointmentTime: appointmentTime + ':00',
      remark: remark
    }).then(() => {
      wx.hideLoading()
      wx.showToast({ title: '预约成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 1500)
    }).catch(() => { wx.hideLoading() })
  }
})
