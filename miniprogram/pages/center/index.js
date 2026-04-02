const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    userInfo: {},
    currentPatient: null
  },

  onShow() {
    this.loadUserInfo();
    this.loadCurrentPatient();
  },

  async loadUserInfo() {
    try {
      const result = await request({
        url: '/api/user/me',
        method: 'GET',
        showLoading: false
      });
      const userInfo = this.formatUserInfo(result.data || {});
      wx.setStorageSync('userInfo', userInfo);
      app.globalData.userInfo = userInfo;
      this.setData({ userInfo });
    } catch (error) {
      const localUserInfo = this.formatUserInfo(wx.getStorageSync('userInfo') || {});
      this.setData({ userInfo: localUserInfo });
    }
  },

  loadCurrentPatient() {
    const currentPatient = wx.getStorageSync('currentPatient') || null;
    app.globalData.currentPatient = currentPatient;
    this.setData({ currentPatient });
  },

  formatUserInfo(userInfo) {
    const formatted = { ...userInfo };
    if (formatted.avatarUrl && formatted.avatarUrl.startsWith('/')) {
      formatted.avatarUrl = app.globalData.baseUrl + formatted.avatarUrl;
    }
    return formatted;
  },

  onAvatarTap() {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath;
        this.uploadAvatar(filePath);
      }
    });
  },

  uploadAvatar(filePath) {
    const token = wx.getStorageSync('token');
    wx.showLoading({ title: '上传中...', mask: true });
    wx.uploadFile({
      url: app.globalData.baseUrl + '/api/user/avatar',
      filePath,
      name: 'file',
      header: {
        Authorization: 'Bearer ' + token
      },
      success: (res) => {
        let result = {};
        try {
          result = JSON.parse(res.data || '{}');
        } catch (e) {
          wx.showToast({ title: '返回数据解析失败', icon: 'none' });
          return;
        }
        if (result.code !== 200) {
          wx.showToast({ title: result.message || '上传失败', icon: 'none' });
          return;
        }
        const userInfo = this.formatUserInfo(result.data || {});
        wx.setStorageSync('userInfo', userInfo);
        app.globalData.userInfo = userInfo;
        this.setData({ userInfo });
        wx.showToast({ title: '头像上传成功', icon: 'success' });
      },
      fail: () => {
        wx.showToast({ title: '上传失败，请重试', icon: 'none' });
      },
      complete: () => {
        wx.hideLoading();
      }
    });
  },

  goProfile() {
    wx.navigateTo({ url: '/pages/profile/edit' });
  },

  goPatient() {
    wx.navigateTo({ url: '/pages/patient/index' });
  },

  onLogout() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出登录吗？',
      success: (res) => {
        if (!res.confirm) return;
        wx.removeStorageSync('token');
        wx.removeStorageSync('userInfo');
        wx.removeStorageSync('userRole');
        wx.removeStorageSync('currentPatient');
        app.globalData.token = null;
        app.globalData.userInfo = null;
        app.globalData.currentPatient = null;
        wx.reLaunch({ url: '/pages/login/login' });
      }
    });
  }
});
