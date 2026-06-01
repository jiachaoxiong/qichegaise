const api = require('../../utils/api')

Page({
  data: {
    photoId: null,
    imageUrl: '',
    displayUrl: '',
    resultUrl: '',
    colors: [],
    categories: [],
    activeCategory: 'all',
    filteredColors: [],
    selectedColor: null,
    isProcessing: false,
    isFailed: false,
    errorMsg: '',
    pollTimer: null
  },

  onLoad(options) {
    const photoId = options.photoId
    const imageUrl = decodeURIComponent(options.imageUrl || '')
    this.setData({ photoId, imageUrl: imageUrl, displayUrl: imageUrl })

    if (photoId && !imageUrl) {
      this.loadPhotoDetail(photoId)
    }

    this.loadColors()
  },

  onUnload() {
    if (this.data.pollTimer) {
      clearInterval(this.data.pollTimer)
    }
  },

  loadPhotoDetail(photoId) {
    api.get('/api/photos').then(works => {
      const work = (works || []).find(w => w.id == photoId)
      if (work) {
        this.setData({
          imageUrl: work.originalUrl,
          displayUrl: work.resultUrl || work.originalUrl,
          resultUrl: work.resultUrl || ''
        })
      }
    })
  },

  loadColors() {
    api.get('/api/colors').then(colors => {
      const categories = [...new Set(colors.map(c => c.category).filter(Boolean))]
      this.setData({
        colors,
        categories,
        filteredColors: colors
      })
    })
  },

  onCategoryTap(e) {
    const cat = e.currentTarget.dataset.cat
    const colors = this.data.colors
    const filtered = cat === 'all'
      ? colors
      : colors.filter(c => c.category === cat)
    this.setData({ activeCategory: cat, filteredColors: filtered })
  },

  onColorTap(e) {
    this.setData({ selectedColor: e.currentTarget.dataset.color })
  },

  onApplyColor() {
    var that = this
    var selectedColor = that.data.selectedColor
    if (!selectedColor) return
    if (!that.data.imageUrl && !that.data.photoId) {
      wx.showToast({ title: '请先上传车辆图片', icon: 'none' })
      return
    }

    that.setData({ isProcessing: true, isFailed: false })

    function doColorize(pid) {
      api.post('/api/ai/colorize', { photoId: parseInt(pid), colorId: selectedColor.id })
        .then(function(result) {
          that.setData({ photoId: result.photoId })
          if (result.status === 'COMPLETED') {
            that.setData({
              displayUrl: result.resultUrl,
              resultUrl: result.resultUrl,
              isProcessing: false
            })
            wx.showToast({ title: '换色完成', icon: 'success' })
          } else if (result.status === 'FAILED') {
            that.setData({ isProcessing: false, isFailed: true, errorMsg: result.errorReason || '处理失败' })
          }
        }).catch(function() {
          that.setData({ isProcessing: false, isFailed: true, errorMsg: '网络异常，请重试' })
        })
    }

    if (that.data.photoId) {
      doColorize(that.data.photoId)
    } else if (that.data.imageUrl) {
      api.post('/api/photos/from-url', { imageUrl: that.data.imageUrl })
        .then(function(data) { doColorize(data.id) })
        .catch(function() { that.setData({ isProcessing: false, isFailed: true, errorMsg: '创建失败' }) })
    }
  },

  startPolling(photoId) {
    let count = 0
    const maxPolls = 30

    const timer = setInterval(() => {
      count++
      api.get('/api/ai/tasks/' + photoId).then(result => {
        if (result.status === 'COMPLETED') {
          clearInterval(timer)
          this.setData({
            displayUrl: result.resultUrl,
            resultUrl: result.resultUrl,
            isProcessing: false,
            pollTimer: null
          })
        } else if (result.status === 'FAILED') {
          clearInterval(timer)
          this.setData({
            isProcessing: false,
            isFailed: true,
            errorMsg: result.errorReason || 'AI 处理失败',
            pollTimer: null
          })
        } else if (count >= maxPolls) {
          clearInterval(timer)
          this.setData({
            isProcessing: false,
            errorMsg: '处理时间较长，请在「我的作品」中查看结果',
            pollTimer: null
          })
        }
      }).catch(() => {
        clearInterval(timer)
        this.setData({ isProcessing: false, pollTimer: null })
      })
    }, 2000)

    this.setData({ pollTimer: timer })
  },

  onRetry() {
    this.setData({ isFailed: false, errorMsg: '' })
  },

  onSave() {
    if (!this.data.resultUrl) return
    wx.showLoading({ title: '保存中...' })
    wx.downloadFile({
      url: this.data.resultUrl,
      success(res) {
        wx.saveImageToPhotosAlbum({
          filePath: res.tempFilePath,
          success() {
            wx.hideLoading()
            wx.showToast({ title: '已保存到相册', icon: 'success' })
          },
          fail() {
            wx.hideLoading()
            wx.showToast({ title: '保存失败，请授权相册权限', icon: 'none' })
          }
        })
      },
      fail() {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      }
    })
  },

  onShare() {
    wx.showToast({ title: '点击右上角菜单分享', icon: 'none' })
  },

  onShareAppMessage() {
    return {
      title: '看看我的改色效果！',
      path: '/pages/color-picker/color-picker?photoId=' + this.data.photoId,
      imageUrl: this.data.resultUrl || this.data.imageUrl
    }
  },

  onShareTimeline() {
    return {
      title: '汽车改色预览效果',
      query: 'photoId=' + this.data.photoId,
      imageUrl: this.data.resultUrl || this.data.imageUrl
    }
  }
})
