const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    userInfo: {},
    currentPatient: null,
    maskedPhone: '—',
    registerTime: '—',
    stats: {
      visitCount: 0,
      revisitPending: 0
    }
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().refresh();
    }
    this.loadUserInfo();
    this.loadCurrentPatient();
    this.applyDerivedFields();
  },

  applyDerivedFields() {
    const userInfo = this.data.userInfo || {};
    const maskedPhone = this.maskPhone(userInfo.phone);
    const registerTime = this.formatRegisterTime(userInfo);
    this.setData({
      maskedPhone,
      registerTime,
      stats: {
        visitCount: 3,
        revisitPending: 1
      }
    });
  },

  maskPhone(phone) {
    if (!phone || phone.length < 7) {
      return '未绑定';
    }
    return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
  },

  formatRegisterTime(userInfo) {
    const t = userInfo.createTime;
    if (!t) {
      return '—';
    }
    if (typeof t === 'string' && t.length >= 10) {
      return t.slice(0, 10);
    }
    return '—';
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
      this.applyDerivedFields();
    } catch (error) {
      const localUserInfo = this.formatUserInfo(wx.getStorageSync('userInfo') || {});
      this.setData({ userInfo: localUserInfo });
      this.applyDerivedFields();
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
        this.applyDerivedFields();
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

  goRecords() {
    wx.navigateTo({ url: '/pages/patient/index' });
  },

  goUpload() {
    wx.navigateTo({ url: '/pages/upload/index' });
  },

  goConsult() {
    wx.navigateTo({ url: '/pages/consult/index' });
  },

  goSettings() {
    wx.navigateTo({ url: '/pages/profile/edit' });
  },

  goHelp() {
    wx.showToast({ title: '帮助中心即将上线', icon: 'none' });
  },

  goAbout() {
    wx.showModal({
      title: '关于我们',
      content: '整形医院服务系统 v1.0.0\n为您提供便捷的就诊与咨询服务。',
      showCancel: false
    });
  },

  onLogoutTap() {
    wx.showModal({
      title: '确认退出',
      content: '确定要退出当前登录账号吗？',
      confirmColor: '#ff4d4f',
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
