const { request } = require('../../utils/request');
const app = getApp();

Page({
  data: {
    phone: '',
    password: '',
    loggingIn: false
  },

  onPhoneInput(e) {
    this.setData({ phone: (e.detail.value || '').trim() });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value || '' });
  },

  validPhone(phone) {
    return /^1\d{10}$/.test(phone);
  },

  async submitLogin() {
    const { phone, password } = this.data;
    if (!this.validPhone(phone)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' });
      return;
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' });
      return;
    }
    this.setData({ loggingIn: true });
    try {
      const result = await request({
        url: '/api/auth/login',
        method: 'POST',
        data: { role: 'patient', phone, password }
      });
      const payload = result.data || {};
      wx.setStorageSync('token', payload.token || '');
      wx.setStorageSync('userInfo', payload.userInfo || {});
      wx.setStorageSync('userRole', 'patient');
      app.globalData.token = payload.token || '';
      app.globalData.userInfo = payload.userInfo || {};
      wx.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 400);
    } catch (err) {
      console.error('患者登录失败', err);
    } finally {
      this.setData({ loggingIn: false });
    }
  }
});
