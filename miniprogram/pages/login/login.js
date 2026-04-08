// pages/login/login.js
const app = getApp();

Page({
  data: {
    agreed: false
  },

  onLoad() {},

  onShow() {
    // 每次进入登录页都清理旧登录态，确保重新走微信授权登录
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('userRole');
    const currentPatient = wx.getStorageSync('currentPatient');
    app.globalData.token = null;
    app.globalData.userInfo = null;
    app.globalData.currentPatient = currentPatient || null;
  },

  // 切换协议勾选
  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  goPatientLogin() {
    if (!this.data.agreed) {
      wx.showToast({ title: '请先阅读并同意用户协议', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/patient/login' });
  },

  goDoctorLogin() {
    if (!this.data.agreed) {
      wx.showToast({ title: '请先阅读并同意用户协议', icon: 'none' });
      return;
    }
    wx.navigateTo({ url: '/pages/doctor/login' });
  },

  openUserAgreement() {
    wx.showModal({
      title: '用户协议',
      content: '本协议旨在规范用户在医院服务平台的使用行为，保护用户隐私及合法权益。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  openPrivacyPolicy() {
    wx.showModal({
      title: '隐私政策',
      content: '我们重视您的隐私，您的个人信息仅用于提供本平台医疗服务，不会泄露给第三方。',
      showCancel: false,
      confirmText: '我知道了'
    });
  }
});

