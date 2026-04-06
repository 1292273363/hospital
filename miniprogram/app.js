// app.js
App({
  globalData: {
    userInfo: null,
    token: null,
    currentPatient: null,
    baseUrl: 'http://8.148.155.226:8080'  // 后端服务地址，根据实际情况修改
  },

  onLaunch() {
    // 检查本地是否有token
    const token = wx.getStorageSync('token');
    const userInfo = wx.getStorageSync('userInfo');
    const currentPatient = wx.getStorageSync('currentPatient');
    if (token) {
      this.globalData.token = token;
      this.globalData.userInfo = userInfo;
      this.globalData.currentPatient = currentPatient || null;
    }
  }
});

