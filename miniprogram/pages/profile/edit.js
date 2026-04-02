const app = getApp();
const { request } = require('../../utils/request');

Page({
  data: {
    submitting: false,
    form: {
      nickName: '',
      phone: '',
      avatarUrl: ''
    }
  },

  onLoad() {
    this.loadUserInfo();
  },

  async loadUserInfo() {
    try {
      const result = await request({
        url: '/api/user/me',
        method: 'GET',
        showLoading: false
      });
      this.setData({
        form: {
          nickName: result.data.nickName || '',
          phone: result.data.phone || '',
          avatarUrl: result.data.avatarUrl || ''
        }
      });
    } catch (error) {
      const localUserInfo = wx.getStorageSync('userInfo') || {};
      this.setData({
        form: {
          nickName: localUserInfo.nickName || '',
          phone: localUserInfo.phone || '',
          avatarUrl: localUserInfo.avatarUrl || ''
        }
      });
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`form.${field}`]: e.detail.value
    });
  },

  async onSubmit() {
    if (!this.data.form.nickName.trim()) {
      wx.showToast({ title: '昵称不能为空', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      const result = await request({
        url: '/api/user/me',
        method: 'PUT',
        data: this.data.form
      });
      const userInfo = result.data || {};
      wx.setStorageSync('userInfo', userInfo);
      app.globalData.userInfo = userInfo;
      wx.showToast({ title: '保存成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
    } catch (error) {
      console.error('更新用户信息失败', error);
    } finally {
      this.setData({ submitting: false });
    }
  }
});
