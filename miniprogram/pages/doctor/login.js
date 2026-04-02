const { request } = require('../../utils/request');
const app = getApp();

Page({
  data: {
    phone: '',
    code: '',
    sendingCode: false,
    countdown: 0,
    loggingIn: false
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  },

  onPhoneInput(e) {
    this.setData({ phone: (e.detail.value || '').trim() });
  },

  onCodeInput(e) {
    this.setData({ code: (e.detail.value || '').trim() });
  },

  validPhone() {
    return /^1\d{10}$/.test(this.data.phone);
  },

  async sendCode() {
    if (!this.validPhone()) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' });
      return;
    }
    this.setData({ sendingCode: true });
    try {
      const result = await request({
        url: '/api/auth/send-code',
        method: 'POST',
        data: {
          role: 'doctor',
          phone: this.data.phone
        }
      });
      const code = result && result.data ? result.data.code : '';
      wx.showModal({
        title: '验证码已发送',
        content: code ? `开发环境验证码：${code}` : '请查看短信验证码',
        showCancel: false
      });
      this.startCountdown();
    } catch (err) {
      console.error('发送验证码失败', err);
    } finally {
      this.setData({ sendingCode: false });
    }
  },

  startCountdown() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
    this.setData({ countdown: 60 });
    this.timer = setInterval(() => {
      const next = this.data.countdown - 1;
      if (next <= 0) {
        clearInterval(this.timer);
        this.timer = null;
        this.setData({ countdown: 0 });
        return;
      }
      this.setData({ countdown: next });
    }, 1000);
  },

  async submitLogin() {
    if (!this.validPhone()) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' });
      return;
    }
    if (!/^\d{6}$/.test(this.data.code)) {
      wx.showToast({ title: '请输入6位验证码', icon: 'none' });
      return;
    }
    this.setData({ loggingIn: true });
    try {
      const result = await request({
        url: '/api/auth/login',
        method: 'POST',
        data: {
          role: 'doctor',
          phone: this.data.phone,
          code: this.data.code
        }
      });
      const payload = result.data || {};
      wx.setStorageSync('token', payload.token || '');
      wx.setStorageSync('userInfo', payload.userInfo || {});
      wx.setStorageSync('userRole', 'doctor');
      app.globalData.token = payload.token || '';
      app.globalData.userInfo = payload.userInfo || {};
      wx.showToast({ title: '登录成功', icon: 'success' });
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/doctor/index' });
      }, 500);
    } catch (err) {
      console.error('医生登录失败', err);
    } finally {
      this.setData({ loggingIn: false });
    }
  }
});
