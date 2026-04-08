const { request } = require('../../utils/request');
const app = getApp();

/** 开发调试开关：生产请保持 false */
const DEV_SKIP_DOCTOR_LOGIN = false;

Page({
  data: {
    loginTab: 'code',
    phone: '',
    code: '',
    password: '',
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

  onTabChange(e) {
    const tab = e.currentTarget.dataset.tab;
    if (tab && tab !== this.data.loginTab) {
      this.setData({ loginTab: tab });
    }
  },

  goPatientLogin() {
    wx.navigateTo({ url: '/pages/patient/login' });
  },

  onForgotPassword() {
    wx.showToast({ title: '请联系管理员重置密码', icon: 'none' });
  },

  onPhoneInput(e) {
    this.setData({ phone: (e.detail.value || '').trim() });
  },

  onCodeInput(e) {
    this.setData({ code: (e.detail.value || '').trim() });
  },

  onPasswordInput(e) {
    this.setData({ password: e.detail.value || '' });
  },

  validPhone(phone) {
    return /^1\d{10}$/.test(phone);
  },

  async sendCode() {
    if (!this.validPhone(this.data.phone)) {
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
      const data = result && result.data ? result.data : {};
      const sms = data.smsCode != null && data.smsCode !== '' ? data.smsCode : data.code;
      wx.showModal({
        title: '验证码已发送',
        content: sms ? `开发环境验证码：${sms}` : '请查看短信验证码',
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
    if (DEV_SKIP_DOCTOR_LOGIN) {
      const mockUserInfo = {
        id: 1,
        doctorName: '开发医生',
        nickName: '开发医生',
        phone: '13800000001'
      };
      const mockToken = 'dev-token-doctor-1';
      wx.setStorageSync('token', mockToken);
      wx.setStorageSync('userInfo', mockUserInfo);
      wx.setStorageSync('userRole', 'doctor');
      app.globalData.token = mockToken;
      app.globalData.userInfo = mockUserInfo;
      wx.showToast({ title: '开发模式已登录', icon: 'success' });
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/doctor/index' });
      }, 400);
      return;
    }

    if (this.data.loginTab === 'password') {
      wx.showToast({
        title: '请使用验证码登录',
        icon: 'none'
      });
      return;
    }

    if (!this.validPhone(this.data.phone)) {
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
