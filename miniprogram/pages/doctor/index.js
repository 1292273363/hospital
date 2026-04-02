const app = getApp();

Page({
  data: {
    userInfo: {}
  },

  onShow() {
    const userInfo = wx.getStorageSync('userInfo') || {};
    this.setData({ userInfo });
  },

  goChat() {
    wx.navigateTo({ url: '/pages/doctor/chat' });
  },

  goCenter() {
    wx.navigateTo({ url: '/pages/doctor/center' });
  },

  logout() {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('userRole');
    app.globalData.token = null;
    app.globalData.userInfo = null;
    wx.reLaunch({ url: '/pages/login/login' });
  },

  goPatientLogin() {
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
