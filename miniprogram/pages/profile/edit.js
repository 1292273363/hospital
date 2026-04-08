const { request } = require('../../utils/request');

Page({
  data: {
    submitting: false,
    form: {
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
  },

  onLoad() {},

  onInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({
      [`form.${field}`]: e.detail.value
    });
  },

  async onSubmit() {
    const { oldPassword, newPassword, confirmPassword } = this.data.form;
    if (!oldPassword || !newPassword || !confirmPassword) {
      wx.showToast({ title: '请填写完整密码信息', icon: 'none' });
      return;
    }
    if (newPassword.length < 4 || newPassword.length > 20) {
      wx.showToast({ title: '新密码长度需4-20位', icon: 'none' });
      return;
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次输入的新密码不一致', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      await request({
        url: '/api/user/password',
        method: 'PUT',
        data: {
          oldPassword,
          newPassword
        }
      });
      wx.showToast({ title: '密码修改成功', icon: 'success' });
      setTimeout(() => {
        wx.navigateBack();
      }, 500);
    } catch (error) {
      console.error('修改密码失败', error);
    } finally {
      this.setData({ submitting: false });
    }
  }
});
