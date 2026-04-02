// pages/login/login.js
const { request } = require('../../utils/request');
const app = getApp();

Page({
  data: {
    isLoading: false,
    agreed: false
  },

  onLoad() {},

  // 切换协议勾选
  toggleAgree() {
    this.setData({ agreed: !this.data.agreed });
  },

  // 微信一键登录（同时获取手机号）
  async onGetPhoneNumber(e) {
    if (!this.data.agreed) {
      wx.showToast({ title: '请先阅读并同意用户协议', icon: 'none' });
      return;
    }
    if (e.detail.errMsg !== 'getPhoneNumber:ok') {
      wx.showToast({ title: '已取消授权手机号，将使用微信登录', icon: 'none' });
      this.setData({ isLoading: true });
      try {
        const loginRes = await this._getWxCode();
        const result = await request({
          url: '/api/wx/login',
          method: 'POST',
          data: { code: loginRes.code }
        });
        this._handleLoginSuccess(result.data);
      } catch (err) {
        console.error('登录失败', err);
      } finally {
        this.setData({ isLoading: false });
      }
      return;
    }

    this.setData({ isLoading: true });
    try {
      // 1. 获取微信code
      const loginRes = await this._getWxCode();
      // 2. 调用后端登录接口（带手机号加密数据）
      const result = await request({
        url: '/api/wx/login',
        method: 'POST',
        data: {
          code: loginRes.code,
          phoneCode: e.detail.code  // 手机号授权code（微信新版）
        }
      });
      this._handleLoginSuccess(result.data);
    } catch (err) {
      console.error('登录失败', err);
    } finally {
      this.setData({ isLoading: false });
    }
  },

  // 获取微信code（Promise化）
  _getWxCode() {
    return new Promise((resolve, reject) => {
      wx.login({
        success: resolve,
        fail: reject
      });
    });
  },

  // 处理登录成功
  _handleLoginSuccess(data) {
    const { token, userInfo } = data;
    // 存储token和用户信息
    wx.setStorageSync('token', token);
    wx.setStorageSync('userInfo', userInfo);
    app.globalData.token = token;
    app.globalData.userInfo = userInfo;

    wx.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      wx.reLaunch({ url: '/pages/index/index' });
    }, 800);
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

