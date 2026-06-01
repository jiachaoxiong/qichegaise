const api = require('../../utils/api')

Page({
  data: { appointments: [] },
  onShow() { this.loadAppointments() },
  loadAppointments() {
    api.get('/api/appointments/my').then(appointments => {
      this.setData({ appointments: appointments || [] })
    }).catch(() => {})
  },
  onCancel(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '取消预约',
      content: '确定要取消这个预约吗？',
      success: (res) => {
        if (res.confirm) {
          api.put('/api/appointments/' + id + '/status', { status: 'CANCELLED' }).then(() => {
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadAppointments()
          })
        }
      }
    })
  }
})
